package net.ihiroky.niotty;

import java.util.concurrent.TimeUnit;

/**
 * Created on 13/01/11, 17:29
 *
 * @author Hiroki Itoh
 */
public class DefaultTransportFuture extends AbstractTransportFuture {

    private volatile Object result_;

    private static final Object DONE = new Object();
    private static final Object CANCELLED = new Object();

    public DefaultTransportFuture(AbstractTransport<?> transport) {
        super(transport);
    }

    @Override
    public boolean isCancelled() {
        return result_ == CANCELLED;
    }

    @Override
    public boolean isDone() {
        return result_ != null;
    }

    @Override
    public boolean isSuccessful() {
        return result_ == DONE;
    }

    @Override
    public Throwable throwable() {
        return (result_ instanceof Throwable) ? (Throwable) result_ : null;
    }

    @Override
    public void throwRuntimeExceptionIfFailed() {
        Throwable t = throwable();
        if (t != null) {
            if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            } else if (t instanceof Exception) {
                throw new RuntimeException(t);
            } else if (t instanceof Error) {
                throw (Error) t;
            }
            throw new Error("Unexpected throwable", t);
        }
    }

    @Override
    public void throwExceptionIfFailed() throws Exception {
        Throwable t = throwable();
        if (t != null) {
            if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            } else if (t instanceof Exception) {
                throw (Exception) t;
            } else if (t instanceof Error) {
                throw (Error) t;
            }
            throw new Error("Unexpected throwable", t);
        }
    }

    @Override
    public TransportFuture waitForCompletion() throws InterruptedException {
        synchronized (this) {
            while (!isDone()) {
                wait();
            }
        }
        return this;
    }

    @Override
    public TransportFuture waitForCompletion(long timeout, TimeUnit unit) throws InterruptedException {
        long left = unit.toMillis(timeout);
        synchronized (this) {
            long start = System.currentTimeMillis();
            long now;
            while (!isDone() && left > 0) {
                wait(left);
                now = System.currentTimeMillis();
                left -= (now - start);
                start = now;
            }
        }
        return this;
    }

    @Override
    public TransportFuture waitForCompletionUninterruptibly() {
        boolean interrupted = false;
        synchronized (this) {
            while (!isDone()) {
                try {
                    wait();
                } catch (InterruptedException ie) {
                    interrupted = true;
                }
            }
        }
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
        return this;
    }

    @Override
    public TransportFuture waitForCompletionUninterruptibly(long timeout, TimeUnit unit) {
        boolean interrupted = false;
        long left = unit.toMillis(timeout);
        synchronized (this) {
            long start = System.currentTimeMillis();
            long now;
            while (!isDone() && left > 0) {
                try {
                    wait(left);
                } catch (InterruptedException ie) {
                    interrupted = true;
                }
                now = System.currentTimeMillis();
                left -= (now - start);
                start = now;
            }
        }
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
        return this;
    }

    public void done() {
        synchronized (this) {
            if (result_ == null) {
                result_ = DONE;
                notifyAll();
            }
        }
        fireOnComplete();
    }

    public void cancel() {
        synchronized (this) {
            if (result_ == null) {
                result_ = CANCELLED;
                notifyAll();
            }
        }
        fireOnComplete();
    }

    public void setThrowable(Throwable t) {
        synchronized (this) {
            if (result_ == null) {
                result_ = t;
                notifyAll();
            }
        }
        fireOnComplete();
    }
}
