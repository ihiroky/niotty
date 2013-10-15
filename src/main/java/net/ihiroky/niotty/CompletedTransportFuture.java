package net.ihiroky.niotty;

import java.util.concurrent.TimeUnit;

/**
 * @author Hiroki Itoh
 */
abstract class CompletedTransportFuture extends AbstractTransportFuture {

    protected CompletedTransportFuture(AbstractTransport<?> transport) {
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
    public TransportFuture waitForCompletion() throws InterruptedException {
        return this;
    }

    @Override
    public TransportFuture waitForCompletion(long timeout, TimeUnit unit) throws InterruptedException {
        return this;
    }

    @Override
    public TransportFuture waitForCompletionUninterruptibly() {
        return this;
    }

    @Override
    public TransportFuture waitForCompletionUninterruptibly(long timeout, TimeUnit unit) {
        return this;
    }
}
