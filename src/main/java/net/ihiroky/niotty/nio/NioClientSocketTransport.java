package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.DefaultTransportFuture;
import net.ihiroky.niotty.FailedTransportFuture;
import net.ihiroky.niotty.PipelineComposer;
import net.ihiroky.niotty.SucceededTransportFuture;
import net.ihiroky.niotty.TransportFuture;
import net.ihiroky.niotty.TransportState;
import net.ihiroky.niotty.buffer.BufferSink;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Objects;

/**
 * @author Hiroki Itoh
 */
public class NioClientSocketTransport extends NioSocketTransport<TcpIOSelector> {

    private final SocketChannel clientChannel_;
    private final ConnectSelectorPool connector_;
    private final WriteQueue writeQueue_;

    NioClientSocketTransport(
            NioClientSocketConfig config, PipelineComposer composer, String name, ConnectSelectorPool connector) {
        Objects.requireNonNull(config, "config");
        Objects.requireNonNull(composer, "composer");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(connector, "connector");

        try {
            SocketChannel clientChannel = SocketChannel.open();
            clientChannel.configureBlocking(false);
            config.applySocketOptions(clientChannel);

            setUpPipelines(name, composer);

            clientChannel_ = clientChannel;
            connector_ = connector;
            writeQueue_ = config.newWriteQueue();
        } catch (Exception e) {
            throw new RuntimeException("failed to open client socket channel.", e);
        }
    }

    NioClientSocketTransport(
            NioServerSocketConfig config, PipelineComposer composer, String name, SocketChannel child) {
        Objects.requireNonNull(config, "config");
        Objects.requireNonNull(composer, "composer");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(child, "child");

        setUpPipelines(name, composer);

        clientChannel_ = child;
        connector_ = null;
        writeQueue_ = config.newWriteQueue();
    }

    @Override
    public void bind(SocketAddress local) throws IOException {
        if (connector_ == null) {
            throw new IllegalStateException("Channel is an accepted channel.");
        }
        clientChannel_.bind(local);
    }

    @Override
    public TransportFuture connect(SocketAddress remote) {
        if (connector_ == null) {
            throw new IllegalStateException("Channel is an accepted channel.");
        }
        try {
            if (clientChannel_.connect(remote)) {
                return new SucceededTransportFuture(this);
            }
            DefaultTransportFuture future = new DefaultTransportFuture(this);
            connector_.register(clientChannel_, SelectionKey.OP_CONNECT, new ConnectionWaitTransport(this, future));
            return future;
        } catch (IOException ioe) {
            return new FailedTransportFuture(this, ioe);
        }
    }

    @Override
    public TransportFuture close() {
        return closeSelectableChannel(TransportState.CONNECTED);
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

    public boolean isConnected() {
        return clientChannel_.isConnected();
    }

    void readyToWrite(AttachedMessage<BufferSink> message) {
        writeQueue_.offer(message);
    }

    int flush() throws IOException {
        return writeQueue_.flushTo(clientChannel_).waitTimeMillis_;
    }

    void fireOnConnect() {
        getTransportListener().onConnect(this, remoteAddress());
    }

    @Override
    void onCloseSelectableChannel() {
        writeQueue_.clear();
    }
}
