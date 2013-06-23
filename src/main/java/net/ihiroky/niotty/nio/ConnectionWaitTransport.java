package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.DefaultTransportFuture;
import net.ihiroky.niotty.PipelineComposer;
import net.ihiroky.niotty.TransportFuture;

import java.net.SocketAddress;

/**
 * @author Hiroki Itoh
 */
public class ConnectionWaitTransport extends NioSocketTransport<ConnectSelector> {

    private NioClientSocketTransport transport_;
    private final DefaultTransportFuture future_;

    ConnectionWaitTransport(NioClientSocketTransport transport, DefaultTransportFuture future) {
        super("ConnectionPending", PipelineComposer.empty(), DEFAULT_WEIGHT);
        transport_ = transport;
        future_ = future;
    }

    NioClientSocketTransport transport() {
        return transport_;
    }

    DefaultTransportFuture getFuture() {
        return future_;
    }

    @Override
    public TransportFuture bind(SocketAddress local) {
        throw new UnsupportedOperationException();
    }

    @Override
    public TransportFuture close() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void write(Object message) {
        throw new UnsupportedOperationException();
    }

    @Override
    public SocketAddress localAddress() {
        throw new UnsupportedOperationException();
    }

    @Override
    public SocketAddress remoteAddress() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isOpen() {
        throw new UnsupportedOperationException();
    }
}
