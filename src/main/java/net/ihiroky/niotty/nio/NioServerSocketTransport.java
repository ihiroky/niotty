package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.DefaultTransportParameter;
import net.ihiroky.niotty.DefaultTransportStateEvent;
import net.ihiroky.niotty.FailedTransportFuture;
import net.ihiroky.niotty.PipelineComposer;
import net.ihiroky.niotty.SuccessfulTransportFuture;
import net.ihiroky.niotty.Transport;
import net.ihiroky.niotty.TransportAggregate;
import net.ihiroky.niotty.TransportAggregateSupport;
import net.ihiroky.niotty.TransportException;
import net.ihiroky.niotty.TransportFuture;
import net.ihiroky.niotty.TransportOption;
import net.ihiroky.niotty.TransportOptions;
import net.ihiroky.niotty.TransportParameter;
import net.ihiroky.niotty.TransportState;
import net.ihiroky.niotty.buffer.BufferSink;
import net.ihiroky.niotty.util.JavaVersion;
import net.ihiroky.niotty.util.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
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

/**
 * An implementation of {@link net.ihiroky.niotty.Transport} for NIO {@code ServerSocketChannel}.
 */
public class NioServerSocketTransport extends NioSocketTransport<AcceptSelector> implements TransportAggregate {

    private ServerSocketChannel serverChannel_;
    private NioServerSocketProcessor processor_;
    private TransportAggregateSupport childAggregate_;
    private final Map<TransportOption<Object>, Object> acceptedSocketOptionMap_;
    private Logger logger_ = LoggerFactory.getLogger(NioServerSocketTransport.class);

    private static Set<TransportOption<?>> SUPPORTED_OPTIONS = Collections.unmodifiableSet(
            new HashSet<TransportOption<?>>(Arrays.<TransportOption<?>>asList(
                    TransportOptions.SO_RCVBUF, TransportOptions.SO_REUSEADDR)));

    public NioServerSocketTransport(NioServerSocketProcessor processor) {
        super(processor.name(), PipelineComposer.empty(), processor.acceptSelectorPool());

        ServerSocketChannel serverChannel = null;
        try {
            serverChannel = ServerSocketChannel.open();
            serverChannel.configureBlocking(false);

            serverChannel_ = serverChannel;
            processor_ = processor;
            childAggregate_ = new TransportAggregateSupport();
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

    public NioServerSocketTransport(NioServerSocketProcessor processor, ServerSocketChannel channel) {
        super(processor.name(), PipelineComposer.empty(), processor.acceptSelectorPool());

        serverChannel_ = channel;
        processor_ = processor;
        childAggregate_ = new TransportAggregateSupport();
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
        childAggregate_.write(message);
    }

    @Override
    public void write(Object message, TransportParameter parameter) {
        childAggregate_.write(message, parameter);
    }

    public void write(Object message, int priority) {
        write(message, new DefaultTransportParameter(priority));
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

    /**
     * Binds the transport's socket to a local address.
     *
     * @param socketAddress the local address
     * @param backlog maximum number of pending connections
     * @return a future object to get the result of this operation
     */
    public TransportFuture bind(SocketAddress socketAddress, int backlog) {
        try {
            if (Platform.javaVersion().ge(JavaVersion.JAVA7)) {
                serverChannel_.bind(socketAddress, backlog);
            } else {
                serverChannel_.socket().bind(socketAddress, backlog);
            }
            processor_.acceptSelectorPool().register(serverChannel_, SelectionKey.OP_ACCEPT, this);
            return new SuccessfulTransportFuture(this);
        } catch (IOException ioe) {
            return new FailedTransportFuture(this, ioe);
        }
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
        future.addListener(childAggregate_);
        return future;
    }

    void registerReadLater(SelectableChannel channel) throws IOException {
        // SocketChannel#getRemoteAddress() may throw IOException, so get remoteAddress first.
        SocketChannel socketChannel = (SocketChannel) channel;
        InetSocketAddress remoteAddress = Platform.javaVersion().ge(JavaVersion.JAVA7)
                ? (InetSocketAddress) socketChannel.getRemoteAddress()
                : (InetSocketAddress) socketChannel.socket().getRemoteSocketAddress();

        TcpIOSelectorPool ioSelectorPool = processor_.ioSelectorPool();
        NioClientSocketTransport child = new NioClientSocketTransport(
                processor_.pipelineComposer(), processor_.name(),
                ioSelectorPool, processor_.writeQueueFactory(), socketChannel);
        synchronized (acceptedSocketOptionMap_) {
            for (Map.Entry<TransportOption<Object>, Object> option : acceptedSocketOptionMap_.entrySet()) {
                child.setOption(option.getKey(), option.getValue());
            }
        }
        for (TransportOption<?> name : child.supportedOptions()) {
            logger_.debug("[registerReadLater] accepted socket's {} = {}", name, child.option(name));
        }

        child.loadEvent(new DefaultTransportStateEvent(TransportState.CONNECTED, remoteAddress));
        ioSelectorPool.register(channel, SelectionKey.OP_READ, child);
        childAggregate_.add(child);
    }

    public Set<Transport> childSet() {
        return childAggregate_.childSet();
    }

    @Override
    void flush(ByteBuffer writeBuffer) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    void readyToWrite(AttachedMessage<BufferSink> message) {
        throw new UnsupportedOperationException();
    }
}
