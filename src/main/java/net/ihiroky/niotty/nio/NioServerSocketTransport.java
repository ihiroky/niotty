package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.DefaultTransportParameter;
import net.ihiroky.niotty.DefaultTransportStateEvent;
import net.ihiroky.niotty.FailedTransportFuture;
import net.ihiroky.niotty.PipelineComposer;
import net.ihiroky.niotty.SuccessfulTransportFuture;
import net.ihiroky.niotty.Transport;
import net.ihiroky.niotty.TransportAggregate;
import net.ihiroky.niotty.TransportAggregateSupport;
import net.ihiroky.niotty.TransportFuture;
import net.ihiroky.niotty.TransportParameter;
import net.ihiroky.niotty.TransportState;
import net.ihiroky.niotty.buffer.BufferSink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketOption;
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.AbstractMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * An implementation of {@link net.ihiroky.niotty.Transport} for NIO {@code ServerSocketChannel}.
 */
public class NioServerSocketTransport extends NioSocketTransport<AcceptSelector> implements TransportAggregate {

    private ServerSocketChannel serverChannel_;
    private NioServerSocketProcessor processor_;
    private TransportAggregateSupport childAggregate_;
    private int backlog_;
    private final Set<Map.Entry<SocketOption<Object>, Object>> acceptedSocketOptionSet_;
    private Logger logger_ = LoggerFactory.getLogger(NioServerSocketTransport.class);

    NioServerSocketTransport(NioServerSocketProcessor processor) {
        super(processor.name(), PipelineComposer.empty(), processor.acceptSelectorPool(), DEFAULT_WEIGHT);

        ServerSocketChannel serverChannel = null;
        try {
            serverChannel = ServerSocketChannel.open();
            serverChannel.configureBlocking(false);

            serverChannel_ = serverChannel;
            processor_ = processor;
            childAggregate_ = new TransportAggregateSupport();
            backlog_ = 0;
            acceptedSocketOptionSet_ = new CopyOnWriteArraySet<>();
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

    /**
     * Set a socket option.
     * @param name the name of the option
     * @param value the value of the option
     * @param <T> the type of the option
     * @return this object
     * @throws IOException if an I/O error occurs
     */
    public <T> NioServerSocketTransport setOption(SocketOption<T> name, T value) throws IOException {
        serverChannel_.setOption(name, value);
        return this;
    }

    /**
     * Returns a socket option value for a specified name.
     * @param name the name
     * @param <T> the type of the value
     * @return the value
     * @throws IOException if I/O error occurs
     */
    public <T> T option(SocketOption<T> name) throws IOException {
        return serverChannel_.getOption(name);
    }

    public NioServerSocketTransport setAcceptedSocketOption(SocketOption<Object> name, Object value) {
        acceptedSocketOptionSet_.add(new AbstractMap.SimpleImmutableEntry<>(name, value));
        return this;
    }

    /**
     * Set a backlog used by {@link #bind(java.net.SocketAddress)}.
     * @param backlog the backlog
     * @return this object
     */
    public NioServerSocketTransport setBacklog(int backlog) {
        backlog_ = backlog;
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
            return (InetSocketAddress) serverChannel_.getLocalAddress();
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    @Override
    public InetSocketAddress remoteAddress() {
        return null;
    }

    @Override
    public boolean isOpen() {
        return serverChannel_.isOpen();
    }

    @Override
    public TransportFuture bind(SocketAddress socketAddress) {
        try {
            serverChannel_.bind(socketAddress, backlog_);
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
        return new SuccessfulTransportFuture(this);
    }

    void registerReadLater(SelectableChannel channel) throws IOException {
        // SocketChannel#getRemoteAddress() may throw IOException, so get remoteAddress first.
        InetSocketAddress remoteAddress = (InetSocketAddress) ((SocketChannel) channel).getRemoteAddress();

        SocketChannel socketChannel = (SocketChannel) channel;
        TcpIOSelectorPool ioSelectorPool = processor_.ioSelectorPool();
        NioClientSocketTransport child = new NioClientSocketTransport(
                processor_.pipelineComposer(), DEFAULT_WEIGHT, processor_.name(),
                ioSelectorPool, processor_.writeQueueFactory(), socketChannel);
        for (Map.Entry<SocketOption<Object>, Object> option : acceptedSocketOptionSet_) {
            socketChannel.setOption(option.getKey(), option.getValue());
        }
        for (SocketOption<?> name : socketChannel.supportedOptions()) {
            logger_.debug("[registerReadLater] accepted socket's {} = {}", name, socketChannel.getOption(name));
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
