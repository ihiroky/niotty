package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.CancelledTransportFuture;
import net.ihiroky.niotty.DefaultTransportFuture;
import net.ihiroky.niotty.DefaultTransportStateEvent;
import net.ihiroky.niotty.FailedTransportFuture;
import net.ihiroky.niotty.PipelineComposer;
import net.ihiroky.niotty.SuccessfulTransportFuture;
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
    private final NioClientSocketProcessor processor_;
    private final WriteQueue writeQueue_;

    NioClientSocketTransport(
            NioClientSocketConfig config, PipelineComposer composer, int weight,
            String name, NioClientSocketProcessor processor) {
        super(name, composer, weight);

        Objects.requireNonNull(config, "config");
        Objects.requireNonNull(processor, "processor");

        try {
            SocketChannel clientChannel = SocketChannel.open();
            clientChannel.configureBlocking(false);
            config.applySocketOptions(clientChannel);

            clientChannel_ = clientChannel;
            processor_ = processor;
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
        processor_ = null;
        writeQueue_ = config.newWriteQueue();
    }

    @Override
    public TransportFuture bind(SocketAddress local) {
        try {
            clientChannel_.bind(local);
            return new SuccessfulTransportFuture(this);
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
        if (clientChannel_.isConnectionPending() || clientChannel_.isConnected()) {
            return new CancelledTransportFuture(this);
        }

        NioClientSocketProcessor processor = processor_;
        if (processor == null) {
            throw new IllegalStateException("No NioClientSocketProcessor is found.");
        }

        if (processor.hasConnectSelector()) {
            // try non blocking connection
            try {
                if (clientChannel_.connect(remote)) {
                    return new SuccessfulTransportFuture(this);
                }
                DefaultTransportFuture future = new DefaultTransportFuture(this);
                processor.connectSelectorPool().register(
                        clientChannel_, SelectionKey.OP_CONNECT, new ConnectionWaitTransport(this, future));
                return future;
            } catch (IOException ioe) {
                return new FailedTransportFuture(this, ioe);
            }
        }

        // try blocking connection
        try {
            clientChannel_.configureBlocking(true);
            clientChannel_.connect(remote);
            clientChannel_.configureBlocking(false);
            loadEvent(new DefaultTransportStateEvent(TransportState.CONNECTED, remote));
            processor.ioSelectorPool().register(clientChannel_, SelectionKey.OP_READ, this);
            return new SuccessfulTransportFuture(this);
        } catch (IOException ioe) {
            try {
                clientChannel_.configureBlocking(false);
            } catch (IOException ioe0) {
                ioe0.printStackTrace();
            }
            return new FailedTransportFuture(this, ioe);
        }
    }

    public boolean isConnected() {
        return clientChannel_.isConnected();
    }

    public TransportFuture shutdownOutput() {
        TcpIOSelector selector = taskLoop();
        if (selector == null) {
            return new SuccessfulTransportFuture(this);
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
        TcpIOSelector selector = taskLoop();
        if (selector == null) {
            return new SuccessfulTransportFuture(this);
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

    @Override
    void onCloseSelectableChannel() {
        writeQueue_.clear();
    }
}
