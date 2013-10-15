package net.ihiroky.niotty;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.*;
import static org.mockito.Mockito.*;

/**
 * @author Hiroki Itoh
 */
public class DefaultTransportFutureTest {

    private DefaultTransportFuture sut_;

    private static ExecutorService executor_;

    @BeforeClass
    public static void setUpClass() {
        executor_ = Executors.newCachedThreadPool();
    }

    @AfterClass
    public static void tearDownClass() {
        executor_.shutdownNow();
        executor_ = null;
    }

    @Before
    public void setUp() {
        @SuppressWarnings("unchecked")
        AbstractTransport<TaskLoop> transport = mock(AbstractTransport.class);
        TaskLoop taskLoop = mock(TaskLoop.class);
        when(taskLoop.isInLoopThread()).thenReturn(true);
        when(transport.taskLoop()).thenReturn(taskLoop);
        sut_ = new DefaultTransportFuture(transport);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testThrowRuntimeExceptionIfFailed_ThrowableIsIndexOutOfBoundException() throws Exception {
        sut_.executing();
        sut_.setThrowable(new IndexOutOfBoundsException());
        sut_.throwRuntimeExceptionIfFailed();
    }

    @Test
    public void testThrowRuntimeExceptionIfFailed_ThrowableIsIOException() throws Exception {
        sut_.executing();
        sut_.setThrowable(new IOException());

        try {
            sut_.throwRuntimeExceptionIfFailed();
        } catch (RuntimeException re) {
            Throwable t = re.getCause();
            assertThat(t, is(instanceOf(IOException.class)));
        }
    }

    @Test(expected = AssertionError.class)
    public void testThrowRuntimeExceptionIfFailed_ThrowableIsAssertionError() throws Exception {
        sut_.executing();
        sut_.setThrowable(new AssertionError());
        sut_.throwRuntimeExceptionIfFailed();
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testThrowExceptionIfFailed_ThrowableIsIndexOutOfBoundException() throws Exception {
        sut_.executing();
        sut_.setThrowable(new IndexOutOfBoundsException());
        sut_.throwExceptionIfFailed();
    }

    @Test(expected = IOException.class)
    public void testThrowExceptionIfFailed_ThrowableIsIOException() throws Exception {
        sut_.executing();
        sut_.setThrowable(new IOException());
        sut_.throwExceptionIfFailed();
    }

    @Test(expected = AssertionError.class)
    public void testThrowExceptionIfFailed_ThrowableIsAssertionError() throws Exception {
        sut_.executing();
        sut_.setThrowable(new AssertionError());
        sut_.throwExceptionIfFailed();
    }

    @Test
    public void testIsCancelled_ReturnsTrueIfCancelled() throws Exception {
        sut_.cancel();
        assertThat(sut_.isCancelled(), is(true));
    }

    @Test
    public void testIsCancelled_ReturnsFalseIfDone() throws Exception {
        sut_.executing();
        sut_.done();
        assertThat(sut_.isCancelled(), is(false));
    }

    @Test
    public void testIsCancelled_ReturnsFalseByDefault() throws Exception {
        assertThat(sut_.isCancelled(), is(false));
    }

    @Test
    public void testIsCancelled_ReturnsFalseIfExecuting() throws Exception {
        sut_.executing();
        assertThat(sut_.isCancelled(), is(false));
    }

    @Test
    public void testIsSuccessful_ReturnsTrueIfDoneAndNoThrowable() throws Exception {
        sut_.executing();
        sut_.done();
        assertThat(sut_.isSuccessful(), is(true));
    }

    @Test
    public void testIsSuccessful_ReturnsTrueIfNotExecutingBeforeDone() throws Exception {
        sut_.done();
        assertThat(sut_.isSuccessful(), is(true));
    }

    @Test
    public void testIsSuccessful_ReturnsFalseIfSetThrowable() throws Exception {
        sut_.executing();
        sut_.setThrowable(new Exception());
        assertThat(sut_.isSuccessful(), is(false));
    }

    @Test
    public void testIsSuccessful_ReturnsFalseByDefault() throws Exception {
        assertThat(sut_.isSuccessful(), is(false));
    }

    @Test
    public void testIsSuccessful_ReturnsFalseIfExecuting() throws Exception {
        sut_.executing();
        assertThat(sut_.isSuccessful(), is(false));
    }
    @Test
    public void testIsDone_ReturnFalseByDefault() throws Exception {
        assertThat(sut_.isDone(), is(false));
    }

    @Test
    public void testIsDone_ReturnTrueIfDone() throws Exception {
        sut_.executing();
        sut_.done();
        assertThat(sut_.isDone(), is(true));
    }

    @Test
    public void testThrowable_ReturnNullIfNotSetThrowable() throws Exception {
        assertThat(sut_.throwable(), is(nullValue()));
    }

    @Test
    public void testThrowable_ReturnInstanceIfSetThrowable() throws Exception {
        Exception e = new Exception();
        sut_.executing();
        sut_.setThrowable(e);
        assertThat(sut_.throwable(), is((Throwable) e));
    }

    @Test
    public void testThrowable_ReturnsInstanceIfNotExecutingBeforeSet() throws Exception {
        Exception e = new Exception();
        sut_.setThrowable(e);
        assertThat(sut_.throwable(), is((Throwable) e));
    }

    @Test(timeout = 1000)
    public void testWaitCompletion() throws Exception {
        long start = System.currentTimeMillis();
        executor_.execute(new Runnable() {
            @Override
            public void run() {
                sut_.executing();
                try {
                    Thread.sleep(10);
                    sut_.done();
                } catch (Exception e) {
                    sut_.setThrowable(e);
                }
            }
        });
        sut_.waitForCompletion();
        long elapsed = System.currentTimeMillis() - start;
        assertThat(Long.toString(elapsed), elapsed >= 10, is(true));
    }

    @Test(expected = InterruptedException.class)
    public void testWaitCompletion_ThrowsInterruptedExceptionIfInterrupted() throws Exception {
        final Thread thread = Thread.currentThread();
        executor_.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(10);
                    thread.interrupt();
                } catch (Exception ignored) {
                }
            }
        });
        sut_.waitForCompletion();
    }

    @Test(timeout = 1000)
    public void testWaitCompletionUninterruptibly() throws Exception {
        long start = System.currentTimeMillis();
        executor_.execute(new Runnable() {
            @Override
            public void run() {
                sut_.executing();
                try {
                    Thread.sleep(10);
                    sut_.done();
                } catch (Exception e) {
                    sut_.setThrowable(e);
                }
            }
        });
        sut_.waitForCompletionUninterruptibly();
        long elapsed = System.currentTimeMillis() - start;
        assertThat(Long.toString(elapsed), elapsed >= 10, is(true));
    }

    @Test
    public void testWaitCompletionUninterruptibly_InterruptedIfInterrupted() throws Exception {
        final Thread thread = Thread.currentThread();
        executor_.execute(new Runnable() {
            @Override
            public void run() {
                sut_.executing();
                try {
                    Thread.sleep(10);
                    thread.interrupt();
                    sut_.done();
                } catch (Exception e) {
                    sut_.setThrowable(e);
                }
            }
        });
        sut_.waitForCompletionUninterruptibly();
        assertThat(Thread.interrupted(), is(true));
    }

    @Test(timeout = 1000)
    public void testWaitCompletionTimeout_InTimeout() throws Exception {
        long start = System.currentTimeMillis();
        executor_.execute(new Runnable() {
            @Override
            public void run() {
                sut_.executing();
                try {
                    Thread.sleep(10);
                    sut_.done();
                } catch (Exception e) {
                    sut_.setThrowable(e);
                }
            }
        });
        sut_.waitForCompletion(100, TimeUnit.MILLISECONDS);
        long elapsed = System.currentTimeMillis() - start;
        assertThat(Long.toString(elapsed), elapsed >= 10 && elapsed <= 100, is(true));
    }

    @Test(timeout = 1000)
    public void testWaitCompletionTimeout_ExceedsTimeout() throws Exception {
        long start = System.currentTimeMillis();
//        executor_.execute(new Runnable() {
//            @Override
//            public void run() {
//                try {
//                    Thread.sleep(100);
//                    sut_.done();
//                } catch (Exception ignored) {
//                }
//            }
//        });
        sut_.waitForCompletion(10, TimeUnit.MILLISECONDS);
        long elapsed = System.currentTimeMillis() - start;
        assertThat(Long.toString(elapsed), elapsed >= 10 && elapsed < 100, is(true));
    }

    @Test(expected = InterruptedException.class, timeout = 1000)
    public void testWaitCompletionTimeout_ThrowsInterruptedExceptionIfInterrupted() throws Exception {
        final Thread thread = Thread.currentThread();
        executor_.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(10);
                    thread.interrupt();
                } catch (Exception ignored) {
                }
            }
        });
        sut_.waitForCompletion(100, TimeUnit.MILLISECONDS);
    }

    @Test(timeout = 1000)
    public void testWaitCompletionUninterruptiblyTimeout_InTimeout() throws Exception {
        long start = System.currentTimeMillis();
        executor_.execute(new Runnable() {
            @Override
            public void run() {
                sut_.executing();
                try {
                    Thread.sleep(10);
                    sut_.done();
                } catch (Exception e) {
                    sut_.setThrowable(e);
                }
            }
        });
        sut_.waitForCompletionUninterruptibly(100, TimeUnit.MILLISECONDS);
        long elapsed = System.currentTimeMillis() - start;
        assertThat(Long.toString(elapsed), elapsed >= 10, is(true));
    }

    @Test(timeout = 1000)
    public void testWaitCompletionUninterruptiblyTimeout_ExceedsTimeout() throws Exception {
        long start = System.currentTimeMillis();
//        executor_.execute(new Runnable() {
//            @Override
//            public void run() {
//                try {
//                    Thread.sleep(10);
//                    sut_.done();
//                } catch (Exception ignored) {
//                }
//            }
//        });
        sut_.waitForCompletionUninterruptibly(10, TimeUnit.MILLISECONDS);
        long elapsed = System.currentTimeMillis() - start;
        assertThat(Long.toString(elapsed), elapsed >= 10, is(true));
    }

    @Test(timeout = 1000)
    public void testWaitCompletionUninterruptiblyTimeout_InterruptedIfInterrupted() throws Exception {
        final Thread thread = Thread.currentThread();
        executor_.execute(new Runnable() {
            @Override
            public void run() {
                sut_.executing();
                try {
                    Thread.sleep(10);
                    thread.interrupt();
                    sut_.done();
                } catch (Exception ignored) {
                }
            }
        });
        sut_.waitForCompletionUninterruptibly(1000, TimeUnit.MILLISECONDS);
        assertThat(Thread.interrupted(), is(true));
    }
}
