package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.CancelledTransportFuture;
import net.ihiroky.niotty.DefaultTransportFuture;
import net.ihiroky.niotty.Event;
import net.ihiroky.niotty.FailedTransportFuture;
import net.ihiroky.niotty.PipelineComposer;
import net.ihiroky.niotty.SuccessfulTransportFuture;
import net.ihiroky.niotty.TransportException;
import net.ihiroky.niotty.TransportFuture;
import net.ihiroky.niotty.TransportOption;
import net.ihiroky.niotty.TransportOptions;
import net.ihiroky.niotty.buffer.Packet;
import net.ihiroky.niotty.util.Arguments;
import net.ihiroky.niotty.util.JavaVersion;
import net.ihiroky.niotty.util.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * An implementation of {@link net.ihiroky.niotty.Transport} for NIO {@code SocketChannel}.
 */
public class NioClientSocketTransport extends NioSocketTransport<SelectDispatcher> {

    private final SocketChannel channel_;
    private final SelectDispatcherGroup connectSelectGroup_;
    private final PacketQueue writeQueue_;
    private FlushStatus flushStatus_;
    private boolean deactivateOnEndOfStream_;

    public enum ShutdownEvent {
        INPUT, OUTPUT
    }

    private static Logger logger_ = LoggerFactory.getLogger(NioClientSocketTransport.class);

    private static final Set<TransportOption<?>> SUPPORTED_OPTIONS = Collections.unmodifiableSet(
            new HashSet<TransportOption<?>>(Arrays.<TransportOption<?>>asList(
                    TransportOptions.SO_RCVBUF, TransportOptions.SO_SNDBUF, TransportOptions.SO_REUSEADDR,
                    TransportOptions.SO_KEEPALIVE, TransportOptions.SO_LINGER, TransportOptions.TCP_NODELAY)));

    public NioClientSocketTransport(String name, PipelineComposer composer,
            SelectDispatcherGroup ioSelectPool, WriteQueueFactory<PacketQueue> writeQueueFactory) {
        this(name, composer, null, ioSelectPool, writeQueueFactory);
    }

    public NioClientSocketTransport(String name, PipelineComposer composer,
            SelectDispatcherGroup connectSelectGroup, SelectDispatcherGroup ioSelectPool,
            WriteQueueFactory<PacketQueue> writeQueueFactory) {
        super(name, composer, ioSelectPool);

        Arguments.requireNonNull(ioSelectPool, "ioPool");
        Arguments.requireNonNull(writeQueueFactory, "writeQueueFactory");

        try {
            SocketChannel clientChannel = SocketChannel.open();
            clientChannel.configureBlocking(false);

            channel_ = clientChannel;
            connectSelectGroup_ = connectSelectGroup;
            writeQueue_ = writeQueueFactory.newWriteQueue();
            deactivateOnEndOfStream_ = true;
        } catch (Exception e) {
            throw new RuntimeException("failed to open client socket channel.", e);
        }
    }

    public NioClientSocketTransport(String name, PipelineComposer composer,
            SelectDispatcherGroup ioSelectGroup, WriteQueueFactory<PacketQueue> writeQueueFactory,
            SocketChannel child) {
        super(name, composer, ioSelectGroup);

        Arguments.requireNonNull(writeQueueFactory, "writeQueueFactory");
        Arguments.requireNonNull(child, "child");

        channel_ = child;
        connectSelectGroup_ = null;
        writeQueue_ = writeQueueFactory.newWriteQueue();
        deactivateOnEndOfStream_ = true;
    }

    /**
     * Sets a socket option.
     * @param option the option
     * @param value the value of the option
     * @param <T> the type of the option
     * @return this object
     * @throws net.ihiroky.niotty.TransportException if an I/O error occurs
     * @throws java.lang.UnsupportedOperationException if the option is not supported
     */
    @Override
    public <T> NioClientSocketTransport setOption(TransportOption<T> option, T value) {
        if (option == TransportOptions.DEACTIVATE_ON_END_OF_STREAM) {
            deactivateOnEndOfStream_ = TransportOptions.DEACTIVATE_ON_END_OF_STREAM.cast(value);
            return this;
        }

        try {
            JavaVersion javaVersion = Platform.javaVersion();
            if (javaVersion.ge(JavaVersion.JAVA7)) {
                if (option == TransportOptions.SO_RCVBUF) {
                    channel_.setOption(StandardSocketOptions.SO_RCVBUF, (Integer) value);
                } else if (option == TransportOptions.SO_SNDBUF) {
                    channel_.setOption(StandardSocketOptions.SO_SNDBUF, (Integer) value);
                } else if (option == TransportOptions.SO_REUSEADDR) {
                    channel_.setOption(StandardSocketOptions.SO_REUSEADDR, (Boolean) value);
                } else if (option == TransportOptions.SO_KEEPALIVE) {
                    channel_.setOption(StandardSocketOptions.SO_KEEPALIVE, (Boolean) value);
                } else if (option == TransportOptions.SO_LINGER) {
                    channel_.setOption(StandardSocketOptions.SO_LINGER, (Integer) value);
                } else if (option == TransportOptions.TCP_NODELAY) {
                    channel_.setOption(StandardSocketOptions.TCP_NODELAY, (Boolean) value);
                } else {
                    throw new UnsupportedOperationException(option.toString());
                }
            } else {
                if (option == TransportOptions.SO_RCVBUF) {
                    channel_.socket().setReceiveBufferSize((Integer) value);
                } else if (option == TransportOptions.SO_SNDBUF) {
                    channel_.socket().setSendBufferSize((Integer) value);
                } else if (option == TransportOptions.SO_REUSEADDR) {
                    channel_.socket().setReuseAddress((Boolean) value);
                } else if (option == TransportOptions.SO_KEEPALIVE) {
                    channel_.socket().setKeepAlive((Boolean) value);
                } else if (option == TransportOptions.SO_LINGER) {
                    Integer i = (Integer) value;
                    if (i != null && i >= 0) {
                        channel_.socket().setSoLinger(true, i);
                    } else {
                        channel_.socket().setSoLinger(false, 0);
                    }
                } else if (option == TransportOptions.TCP_NODELAY) {
                    channel_.socket().setTcpNoDelay((Boolean) value);
                } else {
                    throw new UnsupportedOperationException(option.toString());
                }
            }
        } catch (IOException ioe) {
            throw new TransportException("Failed to set option " + option, ioe);
        }
        return this;
    }

    /**
     * Returns a socket option value for a specified option.
     * @param option the option
     * @param <T> the type of the value
     * @return the value
     * @throws net.ihiroky.niotty.TransportException if I/O error occurs
     * @throws java.lang.UnsupportedOperationException if the option is not supported
     */
    @Override
    public <T> T option(TransportOption<T> option) {
        if (option == TransportOptions.DEACTIVATE_ON_END_OF_STREAM) {
            return option.cast(deactivateOnEndOfStream_);
        }

        try {
            JavaVersion javaVersion = Platform.javaVersion();
            if (javaVersion.ge(JavaVersion.JAVA7)) {
                if (option == TransportOptions.SO_RCVBUF) {
                    return option.cast(channel_.getOption(StandardSocketOptions.SO_RCVBUF));
                } else if (option == TransportOptions.SO_SNDBUF) {
                    return option.cast(channel_.getOption(StandardSocketOptions.SO_SNDBUF));
                } else if (option == TransportOptions.SO_REUSEADDR) {
                    return option.cast(channel_.getOption(StandardSocketOptions.SO_REUSEADDR));
                } else if (option == TransportOptions.SO_KEEPALIVE) {
                    return option.cast(channel_.getOption(StandardSocketOptions.SO_KEEPALIVE));
                } else if (option == TransportOptions.SO_LINGER) {
                    return option.cast(channel_.getOption(StandardSocketOptions.SO_LINGER));
                } else if (option == TransportOptions.TCP_NODELAY) {
                    return option.cast(channel_.getOption(StandardSocketOptions.TCP_NODELAY));
                } else {
                    throw new UnsupportedOperationException(option.toString());
                }
            } else {
                if (option == TransportOptions.SO_RCVBUF) {
                    return option.cast(channel_.socket().getReceiveBufferSize());
                } else if (option == TransportOptions.SO_SNDBUF) {
                    return option.cast(channel_.socket().getSendBufferSize());
                } else if (option == TransportOptions.SO_REUSEADDR) {
                    return option.cast(channel_.socket().getReuseAddress());
                } else if (option == TransportOptions.SO_KEEPALIVE) {
                    return option.cast(channel_.socket().getKeepAlive());
                } else if (option == TransportOptions.SO_LINGER) {
                    return option.cast(channel_.socket().getSoLinger());
                } else if (option == TransportOptions.TCP_NODELAY) {
                    return option.cast(channel_.socket().getTcpNoDelay());
                } else {
                    throw new UnsupportedOperationException(option.toString());
                }
            }
        } catch (IOException ioe) {
            throw new TransportException("Failed to get option " + option, ioe);
        }
    }

    @Override
    public Set<TransportOption<?>> supportedOptions() {
        return SUPPORTED_OPTIONS;
    }

    @Override
    public TransportFuture bind(final SocketAddress local) {
        try {
            boolean bound = Platform.javaVersion().ge(JavaVersion.JAVA7)
                    ? (channel_.getLocalAddress() != null) : channel_.socket().isBound();
            if (bound) {
                return new SuccessfulTransportFuture(this);
            }
        } catch (IOException ioe) {
            return new FailedTransportFuture(this, ioe);
        }

        final DefaultTransportFuture future = new DefaultTransportFuture(this);
        eventDispatcher().execute(new Event() {
            @Override
            public long execute(TimeUnit timeUnit) throws Exception {
                if (future.executing()) {
                    try {
                        SocketChannel channel = channel_;
                        if (Platform.javaVersion().ge(JavaVersion.JAVA7)) {
                            if (channel.getLocalAddress() == null) {
                                channel.bind(local);
                            }
                        } else {
                            Socket socket = channel.socket();
                            if (!socket.isBound()) {
                                socket.bind(local);
                            }
                        }
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

    @Override
    public TransportFuture close() {
        return closeSelectableChannel();
    }

    @Override
    public InetSocketAddress localAddress() {
        try {
            return Platform.javaVersion().ge(JavaVersion.JAVA7)
                    ? (InetSocketAddress) channel_.getLocalAddress()
                    : (InetSocketAddress) channel_.socket().getLocalSocketAddress();
        } catch (IOException ioe) {
            throw new TransportException(ioe);
        }
    }

    @Override
    public InetSocketAddress remoteAddress() {
        try {
            return Platform.javaVersion().ge(JavaVersion.JAVA7)
                    ? (InetSocketAddress) channel_.getRemoteAddress()
                    : (InetSocketAddress) channel_.socket().getRemoteSocketAddress();
        } catch (IOException ioe) {
            throw new TransportException(ioe);
        }
    }

    @Override
    public boolean isOpen() {
        return channel_.isOpen();
    }

    @Override
    public int pendingWriteBuffers() {
        return writeQueue_.size();
    }

    @Override
    public TransportFuture connect(SocketAddress remote) {
        if (channel_.isConnectionPending() || channel_.isConnected()) {
            return new CancelledTransportFuture(this);
        }

        if (connectSelectGroup_ != null && connectSelectGroup_.isOpen()) {
            // Try non blocking connection
            try {
                if (channel_.connect(remote)) {
                    return new SuccessfulTransportFuture(this);
                }
                DefaultTransportFuture future = new DefaultTransportFuture(this);
                ConnectionWaitTransport cwt = new ConnectionWaitTransport(connectSelectGroup_, this, future);
                cwt.register(channel_, SelectionKey.OP_CONNECT); // call pipeline of this, not cwt.
            } catch (IOException ioe) {
                return new FailedTransportFuture(this, ioe);
            }
        }

        // Try blocking connection
        // This operation blocks thread. So don't execute it in I/O thread.
        try {
            channel_.configureBlocking(true);
            channel_.connect(remote);
            channel_.configureBlocking(false);
            register(channel_, SelectionKey.OP_READ);
            return new SuccessfulTransportFuture(this);
        } catch (IOException ioe) {
            return new FailedTransportFuture(this, ioe);
        }
    }

    public boolean isConnected() {
        return channel_.isConnected();
    }

    public TransportFuture shutdownOutput() {
        SelectDispatcher select = eventDispatcher();
        if (select == null) {
            return new SuccessfulTransportFuture(this);
        }
        final DefaultTransportFuture future = new DefaultTransportFuture(this);
        eventDispatcher().execute(new Event() {
            @Override
            public long execute(TimeUnit timeUnit) {
                SelectionKey key = key();
                if (key != null && key.isValid()) {
                    SocketChannel channel = (SocketChannel) key.channel();
                    if (future.executing()) {
                        try {
                            if (Platform.javaVersion().ge(JavaVersion.JAVA7)) {
                                channel.shutdownOutput();
                            } else {
                                channel.socket().shutdownOutput();
                            }
                            pipeline().eventTriggered(ShutdownEvent.OUTPUT);
                            future.done();
                        } catch (IOException ioe) {
                            future.setThrowable(ioe);
                        }
                    }
                }
                return DONE;
            }
        });
        return future;
    }

    public TransportFuture shutdownInput() {
        SelectDispatcher select = eventDispatcher();
        if (select == null) {
            return new SuccessfulTransportFuture(this);
        }
        final DefaultTransportFuture future = new DefaultTransportFuture(this);
        eventDispatcher().execute(new Event() {
            @Override
            public long execute(TimeUnit timeUnit) {
                SelectionKey key = key();
                if (key != null && key.isValid()) {
                    SocketChannel channel = (SocketChannel) key.channel();
                    if (future.executing()) {
                        try {
                            if (Platform.javaVersion().ge(JavaVersion.JAVA7)) {
                                channel.shutdownInput();
                            } else {
                                channel.socket().shutdownInput();
                            }
                            pipeline().eventTriggered(ShutdownEvent.INPUT);
                            future.done();
                        } catch (IOException ioe) {
                            future.setThrowable(ioe);
                        }
                    }
                }
                return DONE;
            }
        });
        return future;

    }

    @Override
    void readyToWrite(Packet message, Object parameter) {
        writeQueue_.offer(message);
    }

    @Override
    void onCloseSelectableChannel() {
        writeQueue_.clear();
    }

    @Override
    void onSelected(SelectionKey key, SelectDispatcher selectDispatcher) {
        assert key == key();

        ReadableByteChannel channel = (ReadableByteChannel) key.channel();
        try {
            if (key.isReadable()) {
                ByteBuffer readBuffer = selectDispatcher.readBuffer_;
                int read = channel.read(readBuffer);
                if (read >= 0) {
                    readBuffer.flip();
                    pipeline().load(readBuffer);
                    readBuffer.clear();
                    return;
                }
                logger_.debug("[onSelected] transport reaches the end of its stream: {}", this);
                if (deactivateOnEndOfStream_) {
                    doCloseSelectableChannel();
                } else {
                    pipeline().eventTriggered(ShutdownEvent.INPUT);
                }
            } else if (key.isWritable()) {
                flushForcibly(selectDispatcher.writeBuffer_);
            }
        } catch (ClosedByInterruptException ie) {
            if (logger_.isDebugEnabled()) {
                logger_.debug("failed to read from transport by interruption:" + this, ie);
            }
            doCloseSelectableChannel();
        } catch (IOException ioe) {
            logger_.error("failed to read from transport:" + this, ioe);
            doCloseSelectableChannel();
        }
    }

    @Override
    void flush(ByteBuffer writeBuffer) throws IOException {
        if (flushStatus_ == FlushStatus.FLUSHING) {
            return;
        }

        flushForcibly(writeBuffer);
    }


    private void flushForcibly(ByteBuffer writeBuffer) throws IOException {
        FlushStatus status = writeQueue_.flush(channel_);
        flushStatus_ = status;
        handleFlushStatus(status);
    }
}
