package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.DefaultTransportFuture;
import net.ihiroky.niotty.PipelineComposer;
import net.ihiroky.niotty.TransportFuture;
import net.ihiroky.niotty.buffer.BufferSink;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;

/**
 * @author Hiroki Itoh
 */
public class ConnectionWaitTransport extends NioSocketTransport<ConnectSelector> {

    private NioClientSocketTransport transport_;
    private final DefaultTransportFuture future_;

    ConnectionWaitTransport(ConnectSelectorPool pool, NioClientSocketTransport transport, DefaultTransportFuture future) {
        super("ConnectionPending", PipelineComposer.empty(), pool, DEFAULT_WEIGHT);
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

    @Override
    void flush(ByteBuffer writeBuffer) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    void readyToWrite(AttachedMessage<BufferSink> message) {
        throw new UnsupportedOperationException();
    }
}
