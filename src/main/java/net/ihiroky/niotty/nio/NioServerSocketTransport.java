package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.Niotty;
import net.ihiroky.niotty.PipeLine;
import net.ihiroky.niotty.TransportAggregate;
import net.ihiroky.niotty.TransportAggregateSupport;
import net.ihiroky.niotty.buffer.BufferSink;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.util.Set;

/**
 * Created on 13/01/10, 14:38
 *
 * @author Hiroki Itoh
 */
public class NioServerSocketTransport extends NioSocketTransport<AcceptSelector> implements TransportAggregate {

    private ServerSocketChannel serverChannel;
    private NioServerSocketProcessor processor;
    private NioServerSocketConfig config;
    private TransportAggregateSupport childAggregate;


    NioServerSocketTransport(NioServerSocketConfig config, NioServerSocketProcessor processor) {
        ServerSocketChannel serverChannel = null;
        try {
            serverChannel = ServerSocketChannel.open();
            serverChannel.configureBlocking(false);
            config.applySocketOptions(serverChannel);

            this.config = config;
            this.serverChannel = serverChannel;
            this.processor = processor;
            this.childAggregate = new TransportAggregateSupport();
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
        childAggregate.write(message);
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
            processor.getAcceptSelectorPool().register(this, serverChannel, SelectionKey.OP_ACCEPT);
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
        NioChildChannelTransport child =
                new NioChildChannelTransport(config, processor.getWriteBufferSize(), processor.isUseDirectBuffer());
        childAggregate.add(child);
        processor.getMessageIOSelectorPool().register(child, channel, ops);
        return child;
    }

    @Override
    protected void writeDirect(BufferSink buffer) {
        throw new UnsupportedOperationException();
    }

    public Set<Transport> childSet() {
        return childAggregate.childSet();
    }
}
