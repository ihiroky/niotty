package net.ihiroky.niotty;

import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 */
public class DefaultTransportFuture extends AbstractTransportFuture {

    private volatile Object result_;

    private static final Object EXECUTING = new Object();
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
    public boolean isExecuting() {
        return result_ == EXECUTING;
    }

    @Override
    public boolean isDone() {
        Object result = result_;
        return result != null && result != EXECUTING;
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
    public TransportFuture await() throws InterruptedException {
        synchronized (this) {
            while (!isDone()) {
                wait();
            }
        }
        return this;
    }

    @Override
    public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
        long left = unit.toNanos(timeout);
        synchronized (this) {
            long base = System.nanoTime();
            long now;
            while (!isDone() && left > 0) {
                TimeUnit.NANOSECONDS.timedWait(this, left);
                now = System.nanoTime();
                left -= (now - base);
                base = now;
            }
        }
        return left > 0;
    }

    @Override
    public TransportFuture awaitUninterruptibly() {
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
    public boolean awaitUninterruptibly(long timeout, TimeUnit unit) {
        boolean interrupted = false;
        long left = unit.toNanos(timeout);
        synchronized (this) {
            long base = System.nanoTime();
            long now;
            while (!isDone() && left > 0) {
                try {
                    TimeUnit.NANOSECONDS.timedWait(this, left);
                } catch (InterruptedException ie) {
                    interrupted = true;
                }
                now = System.nanoTime();
                left -= (now - base);
                base = now;
            }
        }
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
        return left > 0;
    }

    @Override
    public TransportFuture join() {
        synchronized (this) {
            while (!isDone()) {
                try {
                    wait();
                } catch (InterruptedException ie) {
                    throw new TransportException(ie);
                }
            }
        }
        if (isCancelled()) {
            throw new TransportException("Cancelled", new CancellationException());
        }
        Throwable t = throwable();
        if (t != null) {
            throw new TransportException(t);
        }
        return this;
    }

    @Override
    public TransportFuture join(long timeout, TimeUnit unit) {
        long left = unit.toNanos(timeout);
        synchronized (this) {
            long base = System.nanoTime();
            long now;
            while (!isDone() && left > 0) {
                try {
                    TimeUnit.NANOSECONDS.timedWait(this, left);
                } catch (InterruptedException ie) {
                    throw new TransportException(ie);
                }
                now = System.nanoTime();
                left -= (now - base);
                base = now;
            }
        }
        if (left <= 0) {
            throw new TransportException("Timeout", new TimeoutException());
        }
        if (isCancelled()) {
            throw new TransportException("Cancelled", new CancellationException());
        }
        Throwable t = throwable();
        if (t != null) {
            throw new TransportException(t);
        }
        return this;
    }

    /**
     * Changes the state to EXECUTING.
     * @return true if the stage is changed successfully
     */
    public boolean executing() {
        boolean success;
        synchronized (this) {
            success = (result_ == null);
            if (success) {
                result_ = EXECUTING;
                notifyAll();
            }
        }
        return success;
    }

    /**
     * Change the state to DONE.
     * @return true if the state is changed successfully
     */
    public boolean done() {
        boolean success;
        synchronized (this) {
            success = (result_ == null || result_ == EXECUTING);
            if (success) {
                result_ = DONE;
                notifyAll();
            }
        }
        if (success) {
            fireOnComplete();
        }
        return success;
    }

    @Override
    public boolean cancel() {
        boolean success;
        synchronized (this) {
            success = (result_ == null);
            if (success) {
                result_ = CANCELLED;
                notifyAll();
            }
        }
        if (success) {
            fireOnComplete();
        }
        return success;
    }

    /**
     * Changes the state to FAILURE with the specified cause.
     * @param cause the cause
     */
    public boolean setThrowable(Throwable cause) {
        boolean success;
        synchronized (this) {
            success = (result_ == null || result_ == EXECUTING);
            if (success) {
                result_ = cause;
                notifyAll();
            }
        }
        if (success) {
            fireOnComplete();
        }
        return success;
    }
}
