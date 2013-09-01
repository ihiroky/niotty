package net.ihiroky.niotty;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.*;
import static org.mockito.Mockito.*;

/**
 * @author Hiroki Itoh
 */
public class TaskLoopTest {

    private TaskLoopMock sut_;
    private static ExecutorService executor_;
    private static final String THREAD_NAME = "TEST";

    @BeforeClass
    public static void beforeClass() {
        executor_ = Executors.newCachedThreadPool(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, THREAD_NAME);
            }
        });
    }

    @AfterClass
    public static void afterClass() {
        executor_.shutdownNow();
    }

    @Before
    public void setUp() {
        sut_ = new TaskLoopMock();
    }

    @After
    public void tearDown() throws Exception {
        sut_.close();
        while (sut_.isAlive()) {
            Thread.sleep(10);
        }
    }

    @Test(timeout = 1000)
    public void testOfferTask() throws Exception {
        executor_.execute(sut_);
        final boolean[] executed = new boolean[1];
        @SuppressWarnings("unchecked")
        Task t = mock(Task.class);
        doAnswer(new Answer<Long>() {
            @Override
            public Long answer(InvocationOnMock invocation) throws Throwable {
                executed[0] = true;
                return TaskLoop.RETRY_IMMEDIATELY;
            }
        }).when(t).execute(TimeUnit.NANOSECONDS);

        sut_.offer(t);

        while (!executed[0]) {
            Thread.sleep(10);
        }
    }

    @Test(timeout = 1000)
    public void testProcessTask_ExecuteAgainLater() throws Exception {
        executor_.execute(sut_);
        final AtomicInteger counter = new AtomicInteger();

        @SuppressWarnings("unchecked")
        Task t = mock(Task.class);
        doAnswer(new Answer<Long>() {
            @Override
            public Long answer(InvocationOnMock invocation) throws Throwable {
                counter.getAndIncrement();
                return 10L;
            }
        }).when(t).execute(TimeUnit.NANOSECONDS);

        sut_.offer(t);

        // t is retried forever, so check until 10
        while (counter.get() < 10) {
            Thread.sleep(10);
        }
    }

    @Test(timeout = 1000)
    public void testIsInLoopThread_ReturnsTrueIfInTaskLoop() throws Exception {
        executor_.execute(sut_);
        final boolean[] isInLoopThread = new boolean[1];
        @SuppressWarnings("unchecked")
        Task t = mock(Task.class);
        doAnswer(new Answer<Long>() {
            @Override
            public Long answer(InvocationOnMock invocation) throws Throwable {
                isInLoopThread[0] = sut_.isInLoopThread();
                return TaskLoop.RETRY_IMMEDIATELY;
            }
        }).when(t).execute(TimeUnit.NANOSECONDS);

        sut_.offer(t);

        while (!isInLoopThread[0]) {
            Thread.sleep(10);
        }
    }

    @Test
    public void testIsInLoopThread_ReturnsFalseIfNotInTaskLoop() throws Exception {
        assertThat(sut_.isInLoopThread(), is(false));
    }

    @Test(timeout = 1000)
    public void testIsAlive_ReturnsTrueIfSutIsExecuted() throws Exception {
        executor_.execute(sut_);

        while (!sut_.isAlive()) {
            Thread.sleep(10);
        }
    }

    @Test
    public void testIsAlive_ReturnsFalseIfSutIsNotExecuted() throws Exception {
        assertThat(sut_.isAlive(), is(false));
    }

    @Test
    public void testToString_ReturnsThreadNameIfSutIsExecuted() throws Exception {
        executor_.execute(sut_);

        while (!sut_.toString().equals(THREAD_NAME)) {
            Thread.sleep(10);
        }
    }

    @Test
    public void testToString_ReturnsDefaultIfSutIsNotExecuted() throws Exception {
        String actual = sut_.toString();
        assertThat(actual, actual.matches(".*\\.TaskLoopMock@[0-9a-f]+"), is(true));
    }

    @Test
    public void testCompareTo_ReturnsPositiveIfSutIsTheWeigher() throws Exception {
        TaskLoop t = new TaskLoopMock();
        TaskSelection weight1 = mock(TaskSelection.class);
        when(weight1.weight()).thenReturn(1);
        TaskSelection weight2 = mock(TaskSelection.class);
        when(weight2.weight()).thenReturn(2);

        t.accept(weight1);
        sut_.accept(weight2);

        assertThat(sut_.compareTo(t) > 0, is(true));
    }

    @Test
    public void testCompareTo_ReturnsNegativeIfSutIsTheLighter() throws Exception {
        TaskLoop t = new TaskLoopMock();
        TaskSelection weight1 = mock(TaskSelection.class);
        when(weight1.weight()).thenReturn(1);
        TaskSelection weight2 = mock(TaskSelection.class);
        when(weight2.weight()).thenReturn(2);

        t.accept(weight2);
        sut_.accept(weight1);

        assertThat(sut_.compareTo(t) < 0, is(true));
    }

    @Test
    public void testCompareTo_Return0IfTheSameWeight() throws Exception {
        TaskLoop t = new TaskLoopMock();
        TaskSelection weight1 = mock(TaskSelection.class);
        when(weight1.weight()).thenReturn(1);
        TaskSelection weight2 = mock(TaskSelection.class);
        when(weight2.weight()).thenReturn(1);

        t.accept(weight2);
        sut_.accept(weight1);

        assertThat(sut_.compareTo(t), is(0));
    }

    @Test
    public void testAcceptReject_Ordinary() throws Exception {
        TaskSelection weight1 = mock(TaskSelection.class);
        when(weight1.weight()).thenReturn(1);
        TaskSelection weight2 = mock(TaskSelection.class);
        when(weight2.weight()).thenReturn(2);

        assertThat(sut_.weight(), is(0));
        assertThat(sut_.accept(weight1), is(1));
        assertThat(sut_.accept(weight2), is(3));
        assertThat(sut_.weight(), is(3));
        assertThat(sut_.reject(weight1), is(2));
        assertThat(sut_.reject(weight2), is(0));
        assertThat(sut_.selectionView().size(), is(0));
    }

    @Test
    public void testAddWeight_Overflow() throws Exception {
        TaskSelection weight1 = mock(TaskSelection.class);
        when(weight1.weight()).thenReturn(Integer.MAX_VALUE);
        TaskSelection weight2 = mock(TaskSelection.class);
        when(weight2.weight()).thenReturn(2);

        sut_.accept(weight1);
        sut_.accept(weight2);

        assertThat(sut_.weight(), is(Integer.MAX_VALUE));
    }

    @Test
    public void testAddWeight_Underflow() throws Exception {
        TaskSelection weight1 = mock(TaskSelection.class);
        when(weight1.weight()).thenReturn(-1);

        assertThat(sut_.weight(), is(0));
    }

    @Test(timeout = 1000)
    public void testRun_DelayQueueIsPolledTwiceSimultaneously() throws Exception {
        executor_.execute(sut_);
        final boolean[] done = new boolean[2];
        Task task0 = mock(Task.class);
        when(task0.execute(TimeUnit.NANOSECONDS)).thenAnswer(new Answer<Long>() {
            @Override
            public Long answer(InvocationOnMock invocation) throws Throwable {
                done[0] = true;
                return TaskLoop.DONE;
            }
        });
        Task task1 = mock(Task.class);
        when(task1.execute(TimeUnit.NANOSECONDS)).thenAnswer(new Answer<Long>() {
            @Override
            public Long answer(InvocationOnMock invocation) throws Throwable {
                done[1] = true;
                return TaskLoop.DONE;
            }
        });

        TaskFuture e0 = sut_.schedule(task0, 100, TimeUnit.MILLISECONDS);
        TaskFuture e1 = sut_.schedule(task1, 100, TimeUnit.MILLISECONDS);

        while (!done[0] || !done[1]) {
            Thread.sleep(10);
        }
        verify(task0, timeout(120)).execute(TimeUnit.NANOSECONDS);
        assertThat(e0.isDispatched(), is(true));
        assertThat(e0.isCancelled(), is(false));
        verify(task1, timeout(120)).execute(TimeUnit.NANOSECONDS);
        assertThat(e1.isDispatched(), is(true));
        assertThat(e1.isCancelled(), is(false));
    }

    @Test(timeout = 1000)
    public void testRun_DelayQueuePriority() throws Exception {
        executor_.execute(sut_);
        final Queue<Integer> order = new ArrayBlockingQueue<>(10);
        Task task0 = mock(Task.class);
        when(task0.execute(TimeUnit.NANOSECONDS)).thenAnswer(new Answer<Long>() {
            @Override
            public Long answer(InvocationOnMock invocation) throws Throwable {
                order.offer(0);
                return TaskLoop.DONE;
            }
        });
        when(task0.toString()).thenReturn("task0");
        Task task1 = mock(Task.class);
        when(task1.execute(TimeUnit.NANOSECONDS)).thenAnswer(new Answer<Long>() {
            @Override
            public Long answer(InvocationOnMock invocation) throws Throwable {
                order.offer(1);
                return TaskLoop.DONE;
            }
        });
        when(task1.toString()).thenReturn("task1");

        TaskFuture e0 = sut_.schedule(task0, 200, TimeUnit.MILLISECONDS);
        TaskFuture e1 = sut_.schedule(task1, 100, TimeUnit.MILLISECONDS);

        while (order.size() < 2) {
            Thread.sleep(10);
        }
        assertThat(order.poll(), is(1));
        assertThat(order.poll(), is(0));
        verify(task0, timeout(220)).execute(TimeUnit.NANOSECONDS);
        assertThat(e0.isDispatched(), is(true));
        assertThat(e0.isCancelled(), is(false));
        verify(task1, timeout(120)).execute(TimeUnit.NANOSECONDS);
        assertThat(e1.isDispatched(), is(true));
        assertThat(e1.isCancelled(), is(false));
    }

    @Test
    public void testTaskFutureCancel_RemoveHeadEntryInDelayQueue() throws Exception {
        executor_.execute(sut_);
        Task task0 = mock(Task.class);
        when(task0.execute(TimeUnit.NANOSECONDS)).thenAnswer(new Answer<Long>() {
            @Override
            public Long answer(InvocationOnMock invocation) throws Throwable {
                return TaskLoop.DONE;
            }
        });

        TaskFuture e = sut_.schedule(task0, 100, TimeUnit.MILLISECONDS);
        e.cancel();
        while (!e.isDone()) {
            Thread.sleep(10);
        }
        assertThat(e.isDispatched(), is(false));
        assertThat(e.isCancelled(), is(true));
    }

    @Test
    public void testTaskFutureCancel_RemoveHeadEntryInDelayQueueAfterExecute() throws Exception {
        executor_.execute(sut_);
        Task task0 = mock(Task.class);
        when(task0.execute(TimeUnit.NANOSECONDS)).thenAnswer(new Answer<Long>() {
            @Override
            public Long answer(InvocationOnMock invocation) throws Throwable {
                // System.out.println("task0");
                return TaskLoop.DONE;
            }
        });
        Task task1 = mock(Task.class);
        when(task1.execute(TimeUnit.NANOSECONDS)).thenAnswer(new Answer<Long>() {
            @Override
            public Long answer(InvocationOnMock invocation) throws Throwable {
                // System.out.println("task1");
                return TaskLoop.DONE;
            }
        });

        TaskFuture e0 = sut_.schedule(task0, 100, TimeUnit.MILLISECONDS);
        TaskFuture e1 = sut_.schedule(task1, 100, TimeUnit.MILLISECONDS);
        e1.cancel();
        while (!e0.isDone() || !e1.isDone()) {
            // System.out.println(e0 + ", " + e1);
            Thread.sleep(10);
        }
        assertThat(e0.isDispatched(), is(true));
        assertThat(e0.isCancelled(), is(false));
        assertThat(e1.isDispatched(), is(false));
        assertThat(e1.isCancelled(), is(true));
    }

}
