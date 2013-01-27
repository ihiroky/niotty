package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.ContextTransportAggregate;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;

/**
 * Created on 13/01/10, 14:38
 *
 * @author Hiroki Itoh
 */
public class NioServerSocketTransport extends NioSocketTransport<AcceptSelector> {

    private ServerSocketChannel serverChannel;
    private AcceptSelectorPool acceptSelectorPool;
    private MessageIOSelectorPool messageIOSelectorPool;
    private NioServerSocketConfig config;

    NioServerSocketTransport(NioServerSocketConfig cfg,
                             AcceptSelectorPool acceptPool,
                             MessageIOSelectorPool messageIOPool) {
        try {
            config = cfg;
            serverChannel = ServerSocketChannel.open();
            serverChannel.configureBlocking(false);
            cfg.applySocketOptions(serverChannel.socket());
            acceptSelectorPool = acceptPool;
            messageIOSelectorPool = messageIOPool;
            getTransportListener().onOpen(this);
        } catch (IOException ioe) {
            if (serverChannel != null) {
                try {
                    serverChannel.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            throw new RuntimeException("failed to open NioServerSocketTransport.", ioe);
        }
    }

    @Override
    public void write(final Object message) {
        ContextTransportAggregate aggregate = messageIOSelectorPool.getContextTransportAggregate();
        aggregate.write(message);
    }

    @Override
    public void write(Object message, SocketAddress remote) {
        throw new UnsupportedOperationException("write");
    }

    @Override
    public void bind(SocketAddress socketAddress) {
        try {
            serverChannel.bind(socketAddress, config.getBacklog());
            getTransportListener().onBind(this, socketAddress);
            acceptSelectorPool.register(this, serverChannel, SelectionKey.OP_ACCEPT);
        } catch (IOException e) {
            throw new RuntimeException("failed to bind server socket:" + socketAddress, e);
        }
    }

    @Override
    public void connect(SocketAddress remoteAddress) {
        throw new UnsupportedOperationException("connect");
    }

    @Override
    public void close() {
        if (getEventLoop() != null) {
            closeLater();
        }
    }

    @Override
    public void join(InetAddress group, NetworkInterface networkInterface, InetAddress source) {
        throw new UnsupportedOperationException("join");
    }

    NioChildChannelTransport registerLater(SelectableChannel channel, int ops) {
        NioChildChannelTransport child = new NioChildChannelTransport();
        messageIOSelectorPool.register(child, channel, ops);
        return child;
    }

    @Override
    protected void writeDirect(ByteBuffer byteBuffer) {
    }
}
