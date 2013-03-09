package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.DefaultTransportFuture;
import net.ihiroky.niotty.EventLoop;
import net.ihiroky.niotty.FailedTransportFuture;
import net.ihiroky.niotty.SucceededTransportFuture;
import net.ihiroky.niotty.TransportFuture;
import net.ihiroky.niotty.buffer.BufferSink;
import net.ihiroky.niotty.TransportState;
import net.ihiroky.niotty.TransportStateEvent;

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

    private SocketChannel clientChannel_;
    private NioClientSocketConfig config_;
    private NioClientSocketProcessor processor_;
    private volatile NioChildChannelTransport childTransport_;

    public NioClientSocketTransport(NioClientSocketConfig config, NioClientSocketProcessor processor) {
        try {

            SocketChannel clientChannel = SocketChannel.open();
            clientChannel.configureBlocking(false);
            config.applySocketOptions(clientChannel);

            this.clientChannel_ = clientChannel;
            this.config_ = config;
            this.processor_ = processor;
        } catch (Exception e) {
            throw new RuntimeException("failed to open client socket channel.", e);
        }
    }

    @Override
    public TransportFuture bind(SocketAddress local) {
        try {
            clientChannel_.bind(local);
            getTransportListener().onBind(this, local);
            return new SucceededTransportFuture(this);
        } catch (IOException ioe) {
            return new FailedTransportFuture(this, ioe);
        }
    }

    @Override
    public TransportFuture connect(SocketAddress remote) {
        try {
            if (clientChannel_.connect(remote)) {
                return new SucceededTransportFuture(this);
            }
            DefaultTransportFuture future = new DefaultTransportFuture(this);
            processor_.getConnectSelectorPool().register(
                    clientChannel_, SelectionKey.OP_CONNECT, new TransportFutureAttachment<>(this, future));
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
        if (childTransport_ != null) {
            return childTransport_.closeSelectableChannelLater();
        }
        return new SucceededTransportFuture(this);
    }

    @Override
    public TransportFuture join(InetAddress group, NetworkInterface networkInterface, InetAddress source) {
        throw new UnsupportedOperationException("join");
    }

    @Override
    public void write(final Object message) {
        NioChildChannelTransport transport = childTransport_;
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
            return (InetSocketAddress) clientChannel_.getLocalAddress();
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    @Override
    public InetSocketAddress remoteAddress() {
        try {
            return (InetSocketAddress) clientChannel_.getRemoteAddress();
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    @Override
    public boolean isOpen() {
        return clientChannel_.isOpen();
    }

    void registerLater(SelectableChannel channel, int ops, DefaultTransportFuture future) throws IOException {
        NioChildChannelTransport child = processor_.getMessageIOSelectorPool().register(
                config_, processor_.getName(), config_.newWriteQueue(), channel, ops);
        childTransport_ = child;

        InetSocketAddress remoteAddress = (InetSocketAddress) clientChannel_.getRemoteAddress();
        future.done();
        getTransportListener().onConnect(this, remoteAddress);
        child.loadEventLater(new TransportStateEvent(TransportState.CONNECTED, remoteAddress));
    }

    @Override
    protected void writeDirect(final BufferSink buffer) {
        getEventLoop().offerTask(new EventLoop.Task<ConnectSelector>() {
            @Override
            public boolean execute(ConnectSelector eventLoop) throws Exception {
                childTransport_.writeBufferSink(buffer);
                return true;
            }
        });
    }
}
