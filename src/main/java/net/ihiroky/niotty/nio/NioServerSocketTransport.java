package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.DefaultTransportFuture;
import net.ihiroky.niotty.FailedTransportFuture;
import net.ihiroky.niotty.PipelineComposer;
import net.ihiroky.niotty.SuccessfulTransportFuture;
import net.ihiroky.niotty.Task;
import net.ihiroky.niotty.TransportException;
import net.ihiroky.niotty.TransportFuture;
import net.ihiroky.niotty.TransportOption;
import net.ihiroky.niotty.TransportOptions;
import net.ihiroky.niotty.buffer.BufferSink;
import net.ihiroky.niotty.util.JavaVersion;
import net.ihiroky.niotty.util.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.SocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * An implementation of {@link net.ihiroky.niotty.Transport} for NIO {@code ServerSocketChannel}.
 */
public class NioServerSocketTransport extends NioSocketTransport<SelectLoop> {

    private ServerSocketChannel serverChannel_;
    private final String name_;
    private final SelectLoopGroup ioSelectLoopGroup_;
    private final PipelineComposer childPipelineComposer_;
    private final WriteQueueFactory writeQueueFactory_;
    private final Map<TransportOption<Object>, Object> acceptedSocketOptionMap_;
    private Logger logger_ = LoggerFactory.getLogger(NioServerSocketTransport.class);

    private static final Set<TransportOption<?>> SUPPORTED_OPTIONS = Collections.unmodifiableSet(
            new HashSet<TransportOption<?>>(Arrays.<TransportOption<?>>asList(
                    TransportOptions.SO_RCVBUF, TransportOptions.SO_REUSEADDR)));

    public NioServerSocketTransport(String name, PipelineComposer childPipelineComposer,
            SelectLoopGroup acceptSelectLoopGroup, SelectLoopGroup ioSelectLoopGroup,
            WriteQueueFactory writeQueueFactory) {
        super(name, PipelineComposer.empty(), acceptSelectLoopGroup);

        ServerSocketChannel serverChannel = null;
        try {
            serverChannel = ServerSocketChannel.open();
            serverChannel.configureBlocking(false);

            serverChannel_ = serverChannel;
            name_ = name;
            ioSelectLoopGroup_ = ioSelectLoopGroup;
            childPipelineComposer_ = childPipelineComposer;
            writeQueueFactory_ = writeQueueFactory;
            acceptedSocketOptionMap_ = new HashMap<TransportOption<Object>, Object>();
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

    public NioServerSocketTransport(String name, PipelineComposer childPipelineComposer,
            SelectLoopGroup acceptSelectLoopGroup, SelectLoopGroup ioSelectLoopGroup,
            WriteQueueFactory writeQueueFactory, ServerSocketChannel channel) {
        super(name, PipelineComposer.empty(), acceptSelectLoopGroup);

        serverChannel_ = channel;
        name_ = name;
        ioSelectLoopGroup_ = ioSelectLoopGroup;
        childPipelineComposer_ = childPipelineComposer;
        writeQueueFactory_ = writeQueueFactory;
        acceptedSocketOptionMap_ = new HashMap<TransportOption<Object>, Object>();
    }

    /**
     * Set a socket option.
     * @param option the option
     * @param value the value of the option
     * @param <T> the type of the option
     * @return this object
     * @throws net.ihiroky.niotty.TransportException if an I/O error occurs
     */
    public <T> NioServerSocketTransport setOption(TransportOption<T> option, T value) {
        try {
            JavaVersion javaVersion = Platform.javaVersion();
            if (javaVersion.ge(JavaVersion.JAVA7)) {
                if (option == TransportOptions.SO_RCVBUF) {
                    serverChannel_.setOption(StandardSocketOptions.SO_RCVBUF, (Integer) value);
                } else if (option == TransportOptions.SO_REUSEADDR) {
                    serverChannel_.setOption(StandardSocketOptions.SO_REUSEADDR, (Boolean) value);
                } else {
                    throw new UnsupportedOperationException(option.toString());
                }
            } else {
                if (option == TransportOptions.SO_RCVBUF) {
                    serverChannel_.socket().setReceiveBufferSize((Integer) value);
                } else if (option == TransportOptions.SO_REUSEADDR) {
                    serverChannel_.socket().setReuseAddress((Boolean) value);
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
     */
    public <T> T option(TransportOption<T> option) throws TransportException {
        try {
            JavaVersion javaVersion = Platform.javaVersion();
            if (javaVersion.ge(JavaVersion.JAVA7)) {
                if (option == TransportOptions.SO_RCVBUF) {
                    return option.cast(serverChannel_.getOption(StandardSocketOptions.SO_RCVBUF));
                } else if (option == TransportOptions.SO_REUSEADDR) {
                    return option.cast(serverChannel_.getOption(StandardSocketOptions.SO_REUSEADDR));
                } else {
                    throw new UnsupportedOperationException(option.toString());
                }
            } else {
                if (option == TransportOptions.SO_RCVBUF) {
                    return option.cast(serverChannel_.socket().getReceiveBufferSize());
                } else if (option == TransportOptions.SO_REUSEADDR) {
                    return option.cast(serverChannel_.socket().getReuseAddress());
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

    @SuppressWarnings("unchecked")
    public <T> NioServerSocketTransport setAcceptedTransportOption(TransportOption<T> option, T value) {
        synchronized (acceptedSocketOptionMap_) {
            acceptedSocketOptionMap_.put((TransportOption<Object>) option, value);
        }
        return this;
    }

    @Override
    public void write(Object message) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void write(Object message, Object parameter) {
        throw new UnsupportedOperationException();
    }

    @Override
    public InetSocketAddress localAddress() {
        try {
            return Platform.javaVersion().ge(JavaVersion.JAVA7)
                    ? (InetSocketAddress) serverChannel_.getLocalAddress()
                    : (InetSocketAddress) serverChannel_.socket().getLocalSocketAddress();
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    @Override
    public InetSocketAddress remoteAddress() {
        throw new UnsupportedOperationException("remoteAddress");
    }

    @Override
    public boolean isOpen() {
        return serverChannel_.isOpen();
    }

    @Override
    public TransportFuture bind(SocketAddress socketAddress) {
        return bind(socketAddress, 0);
    }

    @Override
    public TransportFuture connect(SocketAddress local) {
        throw new UnsupportedOperationException();
    }

    /**
     * Binds the socket of this transport to a local address, and makes this transport acceptable.
     *
     * @param socketAddress the local address
     * @param backlog maximum number of pending connections
     * @return a future object to get the result of this operation
     */
    public TransportFuture bind(final SocketAddress socketAddress, final int backlog) {
        try {
            boolean bound = Platform.javaVersion().ge(JavaVersion.JAVA7)
                    ? (serverChannel_.getLocalAddress() != null) : serverChannel_.socket().isBound();
            if (bound) {
                return new SuccessfulTransportFuture(this);
            }
        } catch (IOException ioe) {
            return new FailedTransportFuture(this, ioe);
        }


        final DefaultTransportFuture future = new DefaultTransportFuture(this);
        taskLoop().execute(new Task() {
            @Override
            public long execute(TimeUnit timeUnit) throws Exception {
                if (future.executing()) {
                    try {
                        ServerSocketChannel channel = serverChannel_;
                        register(channel, SelectionKey.OP_ACCEPT);
                        if (Platform.javaVersion().ge(JavaVersion.JAVA7)) {
                            if (channel.getLocalAddress() == null) {
                                channel.bind(socketAddress, backlog);
                            }
                        } else {
                            ServerSocket socket = channel.socket();
                            if (!socket.isBound()) {
                                socket.bind(socketAddress, backlog);
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
        if (taskLoop() != null) {
            return closeSelectableChannel();
        }
        try {
            serverChannel_.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        TransportFuture future = new SuccessfulTransportFuture(this);
        return future;
    }

    @Override
    void flush(ByteBuffer writeBuffer) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    void onSelected(SelectionKey key, SelectLoop selectLoop) {
        ServerSocketChannel channel = (ServerSocketChannel) key.channel();
        try {
            SocketChannel acceptedChannel = channel.accept();
            logger_.info("new channel {} is accepted.", acceptedChannel);
            acceptedChannel.configureBlocking(false);
            register(acceptedChannel);
        } catch (IOException ioe) {
            logger_.error("[onSelected] failed to accept channel.", ioe);
        }
    }

    void register(SelectableChannel channel) throws IOException {
        // SocketChannel#getRemoteAddress() may throw IOException, so get remoteAddress first.
        SocketChannel socketChannel = (SocketChannel) channel;
        NioClientSocketTransport acceptedChannel = new NioClientSocketTransport(
                name_, childPipelineComposer_, ioSelectLoopGroup_, writeQueueFactory_, socketChannel);
        synchronized (acceptedSocketOptionMap_) {
            for (Map.Entry<TransportOption<Object>, Object> option : acceptedSocketOptionMap_.entrySet()) {
                acceptedChannel.setOption(option.getKey(), option.getValue());
            }
        }
        for (TransportOption<?> name : acceptedChannel.supportedOptions()) {
            logger_.debug("[register] accepted socket's {} = {}", name, acceptedChannel.option(name));
        }

        acceptedChannel.register(channel, SelectionKey.OP_READ);
    }

    @Override
    void readyToWrite(AttachedMessage<BufferSink> message) {
        throw new UnsupportedOperationException();
    }
}
