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
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.*;
import static org.mockito.Mockito.*;

/**
 *
 */
public class TaskTimerImplTest {

    private TaskTimerImpl sut_;
    private TaskLoopMock taskLoop_;

    private static ExecutorService executor_;

    @BeforeClass
    public static void setUpClass() {
        executor_ = Executors.newSingleThreadExecutor();
    }

    @AfterClass
    public static void tearDownClass() {
        executor_.shutdownNow();
    }

    @Before
    public void setUp() {
        taskLoop_ = new TaskLoopMock();
        sut_ = new TaskTimerImpl("test", 16);
        sut_.start();
    }

    @After
    public void tearDown() {
        sut_.stop();
        taskLoop_.close();
    }

    @Test(timeout = 1000)
    public void testRun_PollTwiceSimultaneously() throws Exception {
        final boolean[] done = new boolean[2];
        executor_.execute(taskLoop_);
        TaskLoop.Task task0 = mock(TaskLoop.Task.class);
        when(task0.execute(TimeUnit.MILLISECONDS)).thenAnswer(new Answer<Long>() {
            @Override
            public Long answer(InvocationOnMock invocation) throws Throwable {
                done[0] = true;
                return TaskLoop.WAIT_NO_LIMIT;
            }
        });
        TaskLoop.Task task1 = mock(TaskLoop.Task.class);
        when(task1.execute(TimeUnit.MILLISECONDS)).thenAnswer(new Answer<Long>() {
            @Override
            public Long answer(InvocationOnMock invocation) throws Throwable {
                done[1] = true;
                return TaskLoop.WAIT_NO_LIMIT;
            }
        });

        sut_.offer(taskLoop_, task0, 100, TimeUnit.MILLISECONDS);
        sut_.offer(taskLoop_, task1, 100, TimeUnit.MILLISECONDS);

        while (!done[0] || !done[1]) {
            Thread.sleep(10);
        }
        verify(task0, timeout(120)).execute(TimeUnit.MILLISECONDS);
        verify(task1, timeout(120)).execute(TimeUnit.MILLISECONDS);
    }

    @Test(timeout = 1000)
    public void testRun_Priority() throws Exception {
        final Queue<Integer> order = new ArrayBlockingQueue<>(10);
        executor_.execute(taskLoop_);
        TaskLoop.Task task0 = mock(TaskLoop.Task.class);
        when(task0.execute(TimeUnit.MILLISECONDS)).thenAnswer(new Answer<Long>() {
            @Override
            public Long answer(InvocationOnMock invocation) throws Throwable {
                order.offer(0);
                return TaskLoop.WAIT_NO_LIMIT;
            }
        });
        when(task0.toString()).thenReturn("task0");
        TaskLoop.Task task1 = mock(TaskLoop.Task.class);
        when(task1.execute(TimeUnit.MILLISECONDS)).thenAnswer(new Answer<Long>() {
            @Override
            public Long answer(InvocationOnMock invocation) throws Throwable {
                order.offer(1);
                return TaskLoop.WAIT_NO_LIMIT;
            }
        });
        when(task1.toString()).thenReturn("task1");

        sut_.offer(taskLoop_, task0, 200, TimeUnit.MILLISECONDS);
        sut_.offer(taskLoop_, task1, 100, TimeUnit.MILLISECONDS);

        while (order.size() < 2) {
            Thread.sleep(10);
        }
        assertThat(order.poll(), is(1));
        assertThat(order.poll(), is(0));
        verify(task0, timeout(220)).execute(TimeUnit.MILLISECONDS);
        verify(task1, timeout(120)).execute(TimeUnit.MILLISECONDS);
    }

    @Test
    public void testFlush() throws Exception {
        assertThat(sut_.flush(TimeUnit.MILLISECONDS), is(-1L));
    }
}
