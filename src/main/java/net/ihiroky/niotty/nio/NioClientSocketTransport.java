package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.DefaultTransportFuture;
import net.ihiroky.niotty.EventLoop;
import net.ihiroky.niotty.FailedTransportFuture;
import net.ihiroky.niotty.SucceededTransportFuture;
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
import java.nio.channels.SocketChannel;

/**
 * Created on 13/01/17, 16:13
 *
 * @author Hiroki Itoh
 */
public class NioClientSocketTransport extends NioSocketTransport<ConnectSelector> {

    private SocketChannel clientChannel;
    private NioClientSocketConfig config;
    private NioClientSocketProcessor processor;
    private volatile NioChildChannelTransport childTransport;

    public NioClientSocketTransport(NioClientSocketConfig config, NioClientSocketProcessor processor) {
        try {

            SocketChannel clientChannel = SocketChannel.open();
            clientChannel.configureBlocking(false);
            config.applySocketOptions(clientChannel);

            this.clientChannel = clientChannel;
            this.config = config;
            this.processor = processor;
        } catch (Exception e) {
            throw new RuntimeException("failed to open client socket channel.", e);
        }
    }

    @Override
    public TransportFuture bind(SocketAddress local) {
        try {
            clientChannel.bind(local);
            getTransportListener().onBind(this, local);
            return new SucceededTransportFuture(this);
        } catch (IOException ioe) {
            return new FailedTransportFuture(this, ioe);
        }
    }

    @Override
    public TransportFuture connect(SocketAddress remote) {
        try {
            if (clientChannel.connect(remote)) {
                return new SucceededTransportFuture(this);
            }
            DefaultTransportFuture future = new DefaultTransportFuture(this);
            processor.getConnectSelectorPool().register(
                    clientChannel, SelectionKey.OP_CONNECT, new TransportFutureAttachment<>(this, future));
            return future;
        } catch (IOException ioe) {
            return new FailedTransportFuture(this, ioe);
        }
    }

    @Override
    public TransportFuture close() {
        if (getEventLoop() == null) {
            closeSelectableChannel();
            return new SucceededTransportFuture(this);
        }

        // SelectionKey of NioClientSocketTransport is already cancelled here.
        if (childTransport != null) {
            return childTransport.closeSelectableChannelLater();
        }
        return new SucceededTransportFuture(this);
    }

    @Override
    public TransportFuture join(InetAddress group, NetworkInterface networkInterface, InetAddress source) {
        throw new UnsupportedOperationException("join");
    }

    @Override
    public void write(final Object message) {
        NioChildChannelTransport transport = childTransport;
        if (transport == null) {
            throw new IllegalStateException("not connected.");
        }
        transport.write(message);
    }

    @Override
    public void write(Object message, SocketAddress remote) {
        throw new UnsupportedOperationException("write");
    }

    @Override
    public InetSocketAddress localAddress() {
        try {
            return (InetSocketAddress) clientChannel.getLocalAddress();
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    @Override
    public InetSocketAddress remoteAddress() {
        try {
            return (InetSocketAddress) clientChannel.getRemoteAddress();
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    @Override
    public boolean isOpen() {
        return clientChannel.isOpen();
    }

    void registerLater(SelectableChannel channel, int ops, DefaultTransportFuture future) {
        InetSocketAddress remoteAddress;
        try {
            remoteAddress = (InetSocketAddress) clientChannel.getRemoteAddress();
            getTransportListener().onConnect(this, remoteAddress);
            future.done();
        } catch (IOException e) {
            future.setThrowable(e);
            return;
        }

        NioChildChannelTransport child =
                new NioChildChannelTransport(config, processor.getWriteBufferSize(), processor.isUseDirectBuffer());
        this.childTransport = child;
        processor.getMessageIOSelectorPool().register(channel, ops, child);
        child.loadEventLater(new TransportStateEvent(child, TransportState.CONNECTED, remoteAddress));
    }

    @Override
    protected void writeDirect(final BufferSink buffer) {
        getEventLoop().offerTask(new EventLoop.Task<ConnectSelector>() {
            @Override
            public boolean execute(ConnectSelector eventLoop) throws Exception {
                childTransport.writeBufferSink(buffer);
                return true;
            }
        });
    }
}
