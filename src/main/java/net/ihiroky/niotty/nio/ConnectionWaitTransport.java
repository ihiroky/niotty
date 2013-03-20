package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.TransportFuture;
import net.ihiroky.niotty.buffer.BufferSink;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketAddress;

/**
 * @author Hiroki Itoh
 */
public class ConnectionWaitTransport extends NioSocketTransport<ConnectSelector> {

    private NioClientSocketTransport transport_;

    ConnectionWaitTransport(NioClientSocketTransport transport) {
        transport_ = transport;
    }

    NioClientSocketTransport transport() {
        return transport_;
    }

    @Override
    protected void writeDirect(BufferSink buffer) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void bind(SocketAddress local) {
        throw new UnsupportedOperationException();
    }

    @Override
    public TransportFuture connect(SocketAddress remote) {
        throw new UnsupportedOperationException();
    }

    @Override
    public TransportFuture close() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void join(InetAddress group, NetworkInterface networkInterface) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void join(InetAddress group, NetworkInterface networkInterface, InetAddress source) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void write(Object message) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void write(Object message, SocketAddress remote) {
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
