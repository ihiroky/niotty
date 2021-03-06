package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.*;
import net.ihiroky.niotty.buffer.Packet;
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
import java.nio.channels.NotYetBoundException;
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

/**
 * An implementation of {@link net.ihiroky.niotty.Transport} for NIO {@code ServerSocketChannel}.
 */
public class NioServerSocketTransport extends NioSocketTransport {

    private ServerSocketChannel serverChannel_;
    private final DefaultPipeline pipeline_;
    private final String name_;
    private final NioEventDispatcherGroup ioEventDispatcherGroup_;
    private final PipelineComposer childPipelineComposer_;
    private final WriteQueueFactory<PacketQueue> writeQueueFactory_;
    private final Map<TransportOption<Object>, Object> acceptedSocketOptionMap_;
    private Logger logger_ = LoggerFactory.getLogger(NioServerSocketTransport.class);

    private static final Set<TransportOption<?>> SUPPORTED_OPTIONS = Collections.unmodifiableSet(
            new HashSet<TransportOption<?>>(Arrays.<TransportOption<?>>asList(
                    TransportOptions.SO_RCVBUF, TransportOptions.SO_REUSEADDR)));

    public NioServerSocketTransport(String name, PipelineComposer childPipelineComposer,
            NioEventDispatcherGroup acceptEventDispatcherGroup,
            NioEventDispatcherGroup ioEventDispatcherGroup,
            WriteQueueFactory<PacketQueue> writeQueueFactory) {
        super(name, PipelineComposer.empty(), acceptEventDispatcherGroup);

        ServerSocketChannel serverChannel = null;
        try {
            serverChannel = ServerSocketChannel.open();
            serverChannel.configureBlocking(false);

            serverChannel_ = serverChannel;
            name_ = name;
            ioEventDispatcherGroup_ = ioEventDispatcherGroup;
            childPipelineComposer_ = childPipelineComposer;
            writeQueueFactory_ = writeQueueFactory;
            acceptedSocketOptionMap_ = new HashMap<TransportOption<Object>, Object>();
            Stage ioStage = ((NioEventDispatcher) eventDispatcher()).ioStage();
            pipeline_ = new DefaultPipeline(name, this, acceptEventDispatcherGroup, Pipeline.IO_STAGE_KEY, ioStage);
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
            NioEventDispatcherGroup acceptEventDispatcherGroup,
            NioEventDispatcherGroup ioEventDispatcherGroup,
            WriteQueueFactory<PacketQueue> writeQueueFactory, ServerSocketChannel channel) {
        super(name, PipelineComposer.empty(), acceptEventDispatcherGroup);

        serverChannel_ = channel;
        name_ = name;
        ioEventDispatcherGroup_ = ioEventDispatcherGroup;
        childPipelineComposer_ = childPipelineComposer;
        writeQueueFactory_ = writeQueueFactory;
        acceptedSocketOptionMap_ = new HashMap<TransportOption<Object>, Object>();
        Stage ioStage = ((NioEventDispatcher) eventDispatcher()).ioStage();
        pipeline_ = new DefaultPipeline(name, this, acceptEventDispatcherGroup, Pipeline.IO_STAGE_KEY, ioStage);
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

    @Override
    public Pipeline pipeline() {
        return pipeline_;
    }

    @Override
    public int pendingWriteBuffers() {
        return 0;
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
        eventDispatcher().execute(new Event() {
            @Override
            public long execute() throws Exception {
                if (future.executing()) {
                    try {
                        ServerSocketChannel channel = serverChannel_;
                        if (Platform.javaVersion().ge(JavaVersion.JAVA7)) {
                            if (channel.getLocalAddress() == null) {
                                channel.bind(socketAddress, backlog);
                                register(channel, SelectionKey.OP_ACCEPT);
                            }
                        } else {
                            ServerSocket socket = channel.socket();
                            if (!socket.isBound()) {
                                socket.bind(socketAddress, backlog);
                                register(channel, SelectionKey.OP_ACCEPT);
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
        if (eventDispatcher() != null) {
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
    void onSelected(SelectionKey key, NioEventDispatcher selectDispatcher) {
        ServerSocketChannel channel = (ServerSocketChannel) key.channel();
        SocketChannel acceptedChannel;
        try {
            acceptedChannel = channel.accept();
            logger_.info("[onSelected] New channel {} is accepted.", acceptedChannel);
        } catch (NotYetBoundException nybe) {
            logger_.error("[onSelected] failed to accept channel.", nybe);
            unregister(); // leave from selector and wait for somebody to bind me.
            return;
        } catch (IOException ioe) {
            logger_.error("[onSelected] failed to accept channel.", ioe);
            doCloseSelectableChannel();
            return;
        }

        try {
            acceptedChannel.configureBlocking(false);
            register(acceptedChannel);
        } catch (IOException ioe) {
            logger_.error("[register] Failed to register channel " + channel);
            try {
                acceptedChannel.close();
            } catch (IOException e) {
                logger_.warn("[onSelected] Failed to close accepted channel.", e);
            }
        }
    }

    void register(SelectableChannel channel) throws IOException {
        // SocketChannel#getRemoteAddress() may throw IOException, so get remoteAddress first.
        SocketChannel socketChannel = (SocketChannel) channel;
        NioClientSocketTransport acceptedChannel = new NioClientSocketTransport(
                name_, childPipelineComposer_, ioEventDispatcherGroup_, writeQueueFactory_, socketChannel);
        synchronized (acceptedSocketOptionMap_) {
            for (Map.Entry<TransportOption<Object>, Object> option : acceptedSocketOptionMap_.entrySet()) {
                acceptedChannel.setOption(option.getKey(), option.getValue());
            }
        }
        for (TransportOption<?> name : acceptedChannel.supportedOptions()) {
            logger_.debug("[register] Accepted socket's {} = {}", name, acceptedChannel.option(name));
        }

        try {
            acceptedChannel.register(channel, SelectionKey.OP_READ);
        } catch (IOException ioe) {
            try {
                acceptedChannel.unregister(); // Not bound yet. So not doCloseSelectableChannel() but unregister().
            } catch (Exception e) {
                logger_.warn("[register] Failed to close accepted channel.", e);
            }
            throw ioe;
        }
    }

    @Override
    void readyToWrite(Packet message, Object parameter) {
        throw new UnsupportedOperationException();
    }
}
