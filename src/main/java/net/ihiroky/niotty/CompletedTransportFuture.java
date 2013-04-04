package net.ihiroky.niotty;

import java.util.concurrent.TimeUnit;

/**
 * @author Hiroki Itoh
 */
abstract class CompletedTransportFuture implements TransportFuture {

    private Transport transport_;

    protected CompletedTransportFuture(Transport transport) {
        this.transport_ = transport;
    }

    @Override
    public Transport transport() {
        return transport_;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isDone() {
        return true;
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
