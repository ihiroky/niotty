package net.ihiroky.niotty;

import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;

/**
 * @author Hiroki Itoh
 */
abstract class CompletedTransportFuture extends AbstractTransportFuture {

    protected CompletedTransportFuture(Transport transport) {
        super(transport);
    }

    @Override
    public boolean isExecuting() {
        return false;
    }

    @Override
    public boolean isDone() {
        return true;
    }

    @Override
    public boolean cancel() {
        return false;
    }

    @Override
    public TransportFuture await() throws InterruptedException {
        return this;
    }

    @Override
    public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
        return true;
    }

    @Override
    public TransportFuture awaitUninterruptibly() {
        return this;
    }

    @Override
    public boolean awaitUninterruptibly(long timeout, TimeUnit unit) {
        return true;
    }

    @Override
    public TransportFuture join() {
        if (isCancelled()) {
            throw new TransportException(new CancellationException());
        }
        Throwable t = throwable();
        if (t != null) {
            throw new TransportException(t);
        }
        return this;
    }

    @Override
    public TransportFuture join(long timeout, TimeUnit unit) {
        join();
        return this;
    }
}
