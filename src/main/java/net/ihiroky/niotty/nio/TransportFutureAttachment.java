package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.DefaultTransportFuture;

/**
 * @author Hiroki Itoh
 */
public class TransportFutureAttachment<S extends AbstractSelector<S>> {

    private final NioSocketTransport<S> transport;
    private final DefaultTransportFuture future;

    public TransportFutureAttachment(NioSocketTransport<S> transport, DefaultTransportFuture future) {
        this.transport = transport;
        this.future = future;
    }

    public NioSocketTransport<S> getTransport() {
        return transport;
    }

    public DefaultTransportFuture getFuture() {
        return future;
    }
}
