package net.ihiroky.niotty;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

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
        executor_ = Executors.newSingleThreadExecutor(new ThreadFactory() {
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
    public void tearDown() {
        sut_.close();
    }

    @Test(timeout = 1000)
    public void testOfferTask() throws Exception {
        executor_.execute(sut_);
        final boolean[] executed = new boolean[1];
        @SuppressWarnings("unchecked")
        TaskLoop.Task<TaskLoopMock> t = mock(TaskLoop.Task.class);
        doAnswer(new Answer<Integer>() {
            @Override
            public Integer answer(InvocationOnMock invocation) throws Throwable {
                executed[0] = true;
                return TaskLoop.TIMEOUT_NOW;
            }
        }).when(t).execute(sut_);

        sut_.offerTask(t);

        while (!sut_.hasNoTask()) {
            Thread.sleep(10);
        }
        while (!executed[0]) {
            Thread.sleep(10);
        }
    }

    @Test(timeout = 1000)
    public void testProcessTask_ExecuteAgainLater() throws Exception {
        executor_.execute(sut_);
        @SuppressWarnings("unchecked")
        TaskLoop.Task<TaskLoopMock> t = mock(TaskLoop.Task.class);
        doReturn(10).when(t).execute(sut_);

        sut_.offerTask(t);

        // The t remains in the task queue.
        // But the t disappears on executing, retry check until it passed.
        for (;;) {
            int i = 0;
            for (; i < 3 & !sut_.hasNoTask(); i++) {
                Thread.sleep(10);
            }
            if (i == 3) {
                break;
            }
        }
    }

    @Test(timeout = 1000)
    public void testIsInLoopThread_ReturnsTrueIfInTaskLoop() throws Exception {
        executor_.execute(sut_);
        final boolean[] isInLoopThread = new boolean[1];
        @SuppressWarnings("unchecked")
        TaskLoop.Task<TaskLoopMock> t = mock(TaskLoop.Task.class);
        doAnswer(new Answer<Integer>() {
            @Override
            public Integer answer(InvocationOnMock invocation) throws Throwable {
                isInLoopThread[0] = sut_.isInLoopThread();
                return TaskLoop.TIMEOUT_NOW;
            }
        }).when(t).execute(sut_);

        sut_.offerTask(t);

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
        assertThat(actual, actual.matches("net\\.ihiroky\\.niotty\\.TaskLoopMock@[0-9a-f]+"), is(true));
    }

    @Test
    public void testCompareTo_ReturnsPositiveIfSutIsTheWeigher() throws Exception {
        TaskLoop<TaskLoopMock> t = new TaskLoopMock();
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
        TaskLoop<TaskLoopMock> t = new TaskLoopMock();
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
        TaskLoop<TaskLoopMock> t = new TaskLoopMock();
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
}
