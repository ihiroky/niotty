package net.ihiroky.niotty;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created on 13/01/11, 17:29
 *
 * @author Hiroki Itoh
 */
public class DefaultTransportFuture implements TransportFuture {

    private final Transport transport;
    private volatile AtomicBoolean done;
    private volatile Throwable throwable;
    private volatile boolean cancelled;

    public DefaultTransportFuture(Transport transport) {
        this.transport = transport;
        this.done = new AtomicBoolean();
    }

    @Override
    public Transport getTransport() {
        return transport;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public boolean isDone() {
        return done.get();
    }

    @Override
    public Throwable getThrowable() {
        return throwable;
    }

    @Override
    public TransportFuture waitForCompletion() throws InterruptedException {
        synchronized (this) {
            while (!done.get()) {
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
            while (!done.get() && left > 0) {
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
            while (!done.get()) {
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
            while (!done.get() && left > 0) {
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
        if (done.compareAndSet(false, true)) {
            synchronized (this) {
                notifyAll();
            }
        }
    }

    public void cancel() {
        cancelled = true;
    }

    public void setThrowable(Throwable t) {
        throwable = t;
    }
}
