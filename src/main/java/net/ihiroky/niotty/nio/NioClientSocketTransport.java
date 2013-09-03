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
import java.net.SocketOption;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * An implementation of {@link net.ihiroky.niotty.Transport} for NIO {@code SocketChannel}.
 */
public class NioClientSocketTransport extends NioSocketTransport<TcpIOSelector> {

    private final SocketChannel clientChannel_;
    private final Pools pools_;
    private final WriteQueue writeQueue_;
    private WriteQueue.FlushStatus flushStatus_;

    private static class Pools {
        final ConnectSelectorPool connectorPool_;
        final TcpIOSelectorPool ioPool_;

        Pools(ConnectSelectorPool connectorPool, TcpIOSelectorPool ioPool) {
            connectorPool_ = connectorPool;
            ioPool_ = ioPool;
        }
    }

    NioClientSocketTransport(
            PipelineComposer composer, int weight, String name,
            ConnectSelectorPool connectorPool, TcpIOSelectorPool ioPool, WriteQueueFactory writeQueueFactory) {
        super(name, composer, ioPool, weight);

        Objects.requireNonNull(connectorPool, "connectorPool");
        Objects.requireNonNull(ioPool, "ioPool");
        Objects.requireNonNull(writeQueueFactory, "writeQueueFactory");

        try {
            SocketChannel clientChannel = SocketChannel.open();
            clientChannel.configureBlocking(false);

            clientChannel_ = clientChannel;
            pools_ = new Pools(connectorPool, ioPool);
            writeQueue_ = writeQueueFactory.newWriteQueue();
        } catch (Exception e) {
            throw new RuntimeException("failed to open client socket channel.", e);
        }
    }

    NioClientSocketTransport(PipelineComposer composer, int weight, String name,
                             TcpIOSelectorPool ioPool, WriteQueueFactory writeQueueFactory, SocketChannel child) {
        super(name, composer, ioPool, weight);

        Objects.requireNonNull(writeQueueFactory, "writeQueueFactory");
        Objects.requireNonNull(child, "child");

        clientChannel_ = child;
        pools_ = null;
        writeQueue_ = writeQueueFactory.newWriteQueue();
    }

    /**
     * Set a socket option.
     * @param name the name of the option
     * @param value the value of the option
     * @param <T> the type of the option
     * @return this object
     * @throws IOException if an I/O error occurs
     */
    public <T> NioClientSocketTransport setOption(SocketOption<T> name, T value) throws IOException {
        clientChannel_.setOption(name, value);
        return this;
    }

    /**
     * Retrun a socket option value for a specified name.
     * @param name the name
     * @param <T> the type of the value
     * @return the value
     * @throws IOException if I/O error occurs
     */
    public <T> T option(SocketOption<T> name) throws IOException {
        return clientChannel_.getOption(name);
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

        Pools pools = pools_;
        if (pools == null) {
            throw new IllegalStateException("No ConnectSelectorPool is found.");
        }

        if (pools.connectorPool_.isOpen()) {
            // try non blocking connection
            try {
                if (clientChannel_.connect(remote)) {
                    return new SuccessfulTransportFuture(this);
                }
                DefaultTransportFuture future = new DefaultTransportFuture(this);
                pools.connectorPool_.register(
                        clientChannel_, SelectionKey.OP_CONNECT,
                        new ConnectionWaitTransport(pools.connectorPool_, this, future));
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
            pools.ioPool_.register(clientChannel_, SelectionKey.OP_READ, this);
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
            public long execute(TimeUnit timeUnit) {
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
                return DONE;
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
            public long execute(TimeUnit timeUnit) {
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
                return DONE;
            }
        });
        return future;

    }

    TcpIOSelectorPool ioSelectorPool() {
        return pools_.ioPool_;
    }

    void readyToWrite(AttachedMessage<BufferSink> message) {
        writeQueue_.offer(message);
    }

    WriteQueue.FlushStatus flush() throws IOException {
        flushStatus_ = writeQueue_.flushTo(clientChannel_);
        return flushStatus_;
    }

    WriteQueue.FlushStatus flushStatus() {
        return flushStatus_;
    }

    @Override
    void onCloseSelectableChannel() {
        writeQueue_.clear();
    }
}
