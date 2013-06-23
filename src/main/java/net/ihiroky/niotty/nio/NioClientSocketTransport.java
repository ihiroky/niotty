package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.DefaultTransportFuture;
import net.ihiroky.niotty.FailedTransportFuture;
import net.ihiroky.niotty.PipelineComposer;
import net.ihiroky.niotty.SucceededTransportFuture;
import net.ihiroky.niotty.TransportFuture;
import net.ihiroky.niotty.TransportState;
import net.ihiroky.niotty.TransportStateEvent;
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
            NioClientSocketConfig config, PipelineComposer composer, int weight,
            String name, ConnectSelectorPool connector) {
        super(name, composer, weight);

        Objects.requireNonNull(config, "config");

        try {
            SocketChannel clientChannel = SocketChannel.open();
            clientChannel.configureBlocking(false);
            config.applySocketOptions(clientChannel);

            clientChannel_ = clientChannel;
            connector_ = connector;
            writeQueue_ = config.newWriteQueue();
        } catch (Exception e) {
            throw new RuntimeException("failed to open client socket channel.", e);
        }
    }

    NioClientSocketTransport(
            NioServerSocketConfig config, PipelineComposer composer, int weight,
            String name, SocketChannel child) {
        super(name, composer, weight);

        Objects.requireNonNull(config, "config");
        Objects.requireNonNull(child, "child");

        clientChannel_ = child;
        connector_ = null;
        writeQueue_ = config.newWriteQueue();
    }

    @Override
    public TransportFuture bind(SocketAddress local) {
        try {
            clientChannel_.bind(local);
            return new SucceededTransportFuture(this);
        } catch (IOException ioe) {
            return new FailedTransportFuture(this, ioe);
        }
    }

    @Override
    public TransportFuture close() {
        return closeSelectableChannel();
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

    public int pendingWriteBuffers() {
        return writeQueue_.size();
    }

    public TransportFuture connect(SocketAddress remote) {
        if (connector_ == null) {
            throw new IllegalStateException(
                    "Channel is an accepted channel or asynchronous connection is not supported.");
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

    public void blockingConnect(SocketAddress remote) throws IOException {
        clientChannel_.configureBlocking(true);
        try {
            clientChannel_.connect(remote);
        } finally {
            clientChannel_.configureBlocking(false);
        }
    }

    public boolean isConnected() {
        return clientChannel_.isConnected();
    }

    public TransportFuture shutdownOutput() {
        TcpIOSelector selector = eventLoop();
        if (selector == null) {
            return new SucceededTransportFuture(this);
        }
        final DefaultTransportFuture future = new DefaultTransportFuture(this);
        executeStore(new TransportStateEvent(TransportState.SHUTDOWN_OUTPUT) {
            @Override
            public void execute() {
                SelectionKey key = key();
                if (key != null && key.isValid()) {
                    SocketChannel channel = (SocketChannel) key.channel();
                    try {
                        channel.shutdownOutput();
                        future.done();
                    } catch (IOException ioe) {
                        future.setThrowable(ioe);
                    }
                }
            }
        });
        return future;
    }

    public TransportFuture shutdownInput() {
        TcpIOSelector selector = eventLoop();
        if (selector == null) {
            return new SucceededTransportFuture(this);
        }
        final DefaultTransportFuture future = new DefaultTransportFuture(this);
        executeStore(new TransportStateEvent(TransportState.SHUTDOWN_INPUT) {
            @Override
            public void execute() {
                SelectionKey key = key();
                if (key != null && key.isValid()) {
                    SocketChannel channel = (SocketChannel) key.channel();
                    try {
                        channel.shutdownInput();
                        future.done();
                    } catch (IOException ioe) {
                        future.setThrowable(ioe);
                    }
                }
            }
        });
        return future;

    }

    void readyToWrite(AttachedMessage<BufferSink> message) {
        writeQueue_.offer(message);
    }

    int flush() throws IOException {
        return writeQueue_.flushTo(clientChannel_).waitTimeMillis_;
    }

    void fireOnConnect() {
        transportListener().onConnect(this, remoteAddress());
    }

    @Override
    void onCloseSelectableChannel() {
        writeQueue_.clear();
    }
}
