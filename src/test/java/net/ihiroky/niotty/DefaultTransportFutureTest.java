package net.ihiroky.niotty;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.*;
import static org.mockito.Mockito.*;

/**
 * @author Hiroki Itoh
 */
public class DefaultTransportFutureTest {

    private DefaultTransportFuture sut_;
    private Listener listener_;

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
        AbstractTransport<EventDispatcher> transport = mock(AbstractTransport.class);
        EventDispatcher eventDispatcher = mock(EventDispatcher.class);
        when(eventDispatcher.isInDispatcherThread()).thenReturn(true);
        when(transport.eventDispatcher()).thenReturn(eventDispatcher);
        sut_ = new DefaultTransportFuture(transport);
        listener_ = new Listener();
    }

    @Test
    public void testSetThrowable_CallListenerOnce() throws Exception {
        sut_.addListener(listener_);
        Exception e = new Exception();
        sut_.setThrowable(e);
        sut_.setThrowable(e);

        assertThat(listener_.count(), is(1));
        assertThat(listener_.future(), is(sut_));
    }

    @Test
    public void testDone_CallListenerOnce() throws Exception {
        sut_.addListener(listener_);
        sut_.done();
        sut_.done();

        assertThat(listener_.count(), is(1));
        assertThat(listener_.future(), is(sut_));
    }

    @Test
    public void testCancel_CallListenerOnce() throws Exception {
        sut_.addListener(listener_);
        sut_.cancel();
        sut_.cancel();

        assertThat(listener_.count(), is(1));
        assertThat(listener_.future(), is(sut_));
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
    public void testAwait() throws Exception {
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
        sut_.await();
        long elapsed = System.currentTimeMillis() - start;
        assertThat(Long.toString(elapsed), elapsed >= 10, is(true));
    }

    @Test(expected = InterruptedException.class)
    public void testAwait_ThrowsInterruptedExceptionIfInterrupted() throws Exception {
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
        sut_.await();
    }

    @Test(timeout = 1000)
    public void testAwaitUninterruptibly() throws Exception {
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
        sut_.awaitUninterruptibly();
        long elapsed = System.currentTimeMillis() - start;
        assertThat(Long.toString(elapsed), elapsed >= 10, is(true));
    }

    @Test
    public void testAwaitUninterruptibly_InterruptedIfInterrupted() throws Exception {
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
        sut_.awaitUninterruptibly();
        assertThat(Thread.interrupted(), is(true));
    }

    @Test(timeout = 1000)
    public void testAwaitTimeout_InTimeout() throws Exception {
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
        sut_.await(100, TimeUnit.MILLISECONDS);
        long elapsed = System.currentTimeMillis() - start;
        assertThat(Long.toString(elapsed), elapsed >= 10 && elapsed <= 100, is(true));
    }

    @Test(timeout = 1000)
    public void testAwaitTimeout_ExceedsTimeout() throws Exception {
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
        sut_.await(10, TimeUnit.MILLISECONDS);
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
        sut_.await(100, TimeUnit.MILLISECONDS);
    }

    @Test(timeout = 1000)
    public void testAwaitUninterruptiblyTimeout_InTimeout() throws Exception {
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
        sut_.awaitUninterruptibly(100, TimeUnit.MILLISECONDS);
        long elapsed = System.currentTimeMillis() - start;
        assertThat(Long.toString(elapsed), elapsed >= 10, is(true));
    }

    @Test(timeout = 1000)
    public void testAwaitUninterruptiblyTimeout_ExceedsTimeout() throws Exception {
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
        sut_.awaitUninterruptibly(10, TimeUnit.MILLISECONDS);
        long elapsed = System.currentTimeMillis() - start;
        assertThat(Long.toString(elapsed), elapsed >= 10, is(true));
    }

    @Test(timeout = 1000)
    public void testAwaitUninterruptiblyTimeout_InterruptedIfInterrupted() throws Exception {
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
        sut_.awaitUninterruptibly(1000, TimeUnit.MILLISECONDS);
        assertThat(Thread.interrupted(), is(true));
    }

    @Test
    public void testJoin_ExceptionIfInterrupted() throws Exception {
        final Thread thread = Thread.currentThread();
        executor_.execute(new Runnable() {
            @Override
            public void run() {
                sut_.executing();
                try {
                    Thread.sleep(10);
                    thread.interrupt();
                } catch (Exception ignored) {
                }
            }
        });
        try {
            sut_.join();
            throw new AssertionError();
        } catch (TransportException te) {
            assertThat(te.getCause(), is(instanceOf(InterruptedException.class)));
        }
    }

    @Test
    public void testJoin_ExceptionIfCancelled() throws Exception {
        sut_.cancel();
        try {
            sut_.join();
            throw new AssertionError();
        } catch (TransportException te) {
            assertThat(te.getCause(), is(instanceOf(CancellationException.class)));
        }
    }

    @Test
    public void testJoin_ExceptionIfFailed() throws Exception {
        executor_.execute(new Runnable() {
            @Override
            public void run() {
                sut_.executing();
                try {
                    Thread.sleep(10);
                    sut_.setThrowable(new IOException());
                } catch (InterruptedException ignored) {
                }
            }
        });
        try {
            sut_.join();
            throw new AssertionError();
        } catch (TransportException te) {
            assertThat(te.getCause(), is(instanceOf(IOException.class)));
        }
    }

    @Test
    public void testJoin_Done() throws Exception {
        executor_.execute(new Runnable() {
            @Override
            public void run() {
                sut_.executing();
                try {
                    Thread.sleep(10);
                    sut_.done();
                } catch (InterruptedException ignored) {
                }
            }
        });
        sut_.join();
    }

    @Test
    public void testJoinTimeout_ExceptionIfInterrupted() throws Exception {
        final Thread thread = Thread.currentThread();
        executor_.execute(new Runnable() {
            @Override
            public void run() {
                sut_.executing();
                try {
                    Thread.sleep(10);
                    thread.interrupt();
                } catch (Exception ignored) {
                }
            }
        });
        try {
            sut_.join(1000, TimeUnit.MILLISECONDS);
            throw new AssertionError();
        } catch (TransportException te) {
            assertThat(te.getCause(), is(instanceOf(InterruptedException.class)));
        }
    }

    @Test
    public void testJoinTimeout_ExceptionIfCancelled() throws Exception {
        sut_.cancel();
        try {
            sut_.join(1000, TimeUnit.MILLISECONDS);
            throw new AssertionError();
        } catch (TransportException te) {
            assertThat(te.getCause(), is(instanceOf(CancellationException.class)));
        }
    }

    @Test
    public void testJoinTimeout_ExceptionIfFailed() throws Exception {
        executor_.execute(new Runnable() {
            @Override
            public void run() {
                sut_.executing();
                try {
                    Thread.sleep(10);
                    sut_.setThrowable(new IOException());
                } catch (InterruptedException ignored) {
                }
            }
        });
        try {
            sut_.join(1000, TimeUnit.MILLISECONDS);
            throw new AssertionError();
        } catch (TransportException te) {
            assertThat(te.getCause(), is(instanceOf(IOException.class)));
        }
    }

    @Test
    public void testJoinTimeout_Done() throws Exception {
        executor_.execute(new Runnable() {
            @Override
            public void run() {
                sut_.executing();
                try {
                    Thread.sleep(10);
                    sut_.done();
                } catch (InterruptedException ignored) {
                }
            }
        });
        sut_.join(1000, TimeUnit.MILLISECONDS);
    }

    @Test
    public void testJoinTimeout_ExceptionIfTimeout() throws Exception {
        try {
            sut_.join(10, TimeUnit.MILLISECONDS);
            throw new AssertionError();
        } catch (TransportException te) {
            assertThat(te.getCause(), is(instanceOf(TimeoutException.class)));
        }
    }

    private static class Listener implements CompletionListener {

        DefaultTransportFuture future_;
        int counter_;

        @Override
        public void onComplete(TransportFuture future) {
            synchronized (this) {
                future_ = (DefaultTransportFuture) future;
                counter_++;
                notifyAll();
            }
        }

        DefaultTransportFuture future() throws InterruptedException {
            synchronized (this) {
                while (future_ == null) {
                    wait();
                }
            }
            return future_;
        }

        int count() {
            synchronized (this) {
                return counter_;
            }
        }
    }
}
