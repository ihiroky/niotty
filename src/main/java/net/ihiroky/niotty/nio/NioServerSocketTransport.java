package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.DefaultTransportFuture;
import net.ihiroky.niotty.FailedTransportFuture;
import net.ihiroky.niotty.SucceededTransportFuture;
import net.ihiroky.niotty.Transport;
import net.ihiroky.niotty.TransportAggregate;
import net.ihiroky.niotty.TransportAggregateSupport;
import net.ihiroky.niotty.TransportFuture;
import net.ihiroky.niotty.buffer.BufferSink;
import net.ihiroky.niotty.event.TransportState;
import net.ihiroky.niotty.event.TransportStateEvent;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
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
    public InetSocketAddress localAddress() {
        try {
            return (InetSocketAddress) serverChannel.getLocalAddress();
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    @Override
    public InetSocketAddress remoteAddress() {
        return null;
    }

    @Override
    public boolean isOpen() {
        return serverChannel.isOpen();
    }

    @Override
    public TransportFuture bind(SocketAddress socketAddress) {
        try {
            serverChannel.bind(socketAddress, config.getBacklog());
            getTransportListener().onBind(this, socketAddress);
            DefaultTransportFuture future = new DefaultTransportFuture(this);
            processor.getAcceptSelectorPool().register(
                    serverChannel, SelectionKey.OP_ACCEPT, new TransportFutureAttachment<>(this, future));
            return future;
        } catch (IOException e) {
            return new FailedTransportFuture(this, e);
        }
    }

    @Override
    public TransportFuture connect(SocketAddress remoteAddress) {
        throw new UnsupportedOperationException("connect");
    }

    @Override
    public TransportFuture close() {
        if (getEventLoop() != null) {
            return closeSelectableChannelLater();
        }
        return new SucceededTransportFuture(this);
    }

    @Override
    public TransportFuture join(InetAddress group, NetworkInterface networkInterface, InetAddress source) {
        throw new UnsupportedOperationException("join");
    }

    void registerLater(SelectableChannel channel, int ops, DefaultTransportFuture future) {
        InetSocketAddress remoteAddress;
        try {
            remoteAddress = (InetSocketAddress) ((SocketChannel)channel).getRemoteAddress();
            getTransportListener().onConnect(this, remoteAddress());
            future.done();
        } catch (IOException ioe) {
            future.setThrowable(ioe);
            return;
        }

        NioChildChannelTransport child =
                new NioChildChannelTransport(config, processor.getName(),
                        processor.getWriteBufferSize(), processor.isUseDirectBuffer());
        childAggregate.add(child);
        processor.getMessageIOSelectorPool().register(channel, ops, child);
        child.loadEventLater(new TransportStateEvent(child, TransportState.ACCEPTED, remoteAddress));
    }

    @Override
    protected void writeDirect(BufferSink buffer) {
        throw new UnsupportedOperationException();
    }

    public Set<Transport> childSet() {
        return childAggregate.childSet();
    }
}
