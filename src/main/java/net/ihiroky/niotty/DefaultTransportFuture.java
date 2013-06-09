package net.ihiroky.niotty;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created on 13/01/11, 17:29
 *
 * @author Hiroki Itoh
 */
public class DefaultTransportFuture implements TransportFuture {

    private final Transport transport_;
    private volatile AtomicBoolean done_;
    private volatile Throwable throwable_;
    private volatile boolean cancelled_;

    public DefaultTransportFuture(Transport transport) {
        this.transport_ = transport;
        this.done_ = new AtomicBoolean();
    }

    @Override
    public Transport transport() {
        return transport_;
    }

    @Override
    public boolean isCancelled() {
        return cancelled_;
    }

    @Override
    public boolean isDone() {
        return done_.get();
    }

    @Override
    public Throwable throwable() {
        return throwable_;
    }

    @Override
    public void throwIfFailed() {
        if (throwable_ != null) {
            if (throwable_ instanceof RuntimeException) {
                throw (RuntimeException) throwable_;
            } else if (throwable_ instanceof Error) {
                throw (Error) throwable_;
            } else {
                throw new RuntimeException(throwable_);
            }
        }
    }

    @Override
    public TransportFuture waitForCompletion() throws InterruptedException {
        synchronized (this) {
            while (!done_.get()) {
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
            while (!done_.get() && left > 0) {
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
            while (!done_.get()) {
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
            while (!done_.get() && left > 0) {
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
        if (done_.compareAndSet(false, true)) {
            synchronized (this) {
                notifyAll();
            }
        }
    }

    public void cancel() {
        cancelled_ = true;
        done();
    }

    public void setThrowable(Throwable t) {
        throwable_ = t;
        done();
    }
}
