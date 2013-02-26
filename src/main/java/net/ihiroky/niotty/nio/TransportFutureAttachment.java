package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.DefaultTransportFuture;

/**
 * @author Hiroki Itoh
 */
public class TransportFutureAttachment<S extends AbstractSelector<S>> {

    private final NioSocketTransport<S> transport_;
    private final DefaultTransportFuture future_;

    public TransportFutureAttachment(NioSocketTransport<S> transport, DefaultTransportFuture future) {
        this.transport_ = transport;
        this.future_ = future;
    }

    public NioSocketTransport<S> getTransport() {
        return transport_;
    }

    public DefaultTransportFuture getFuture() {
        return future_;
    }
}
