package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.DefaultTransportParameter;
import net.ihiroky.niotty.DefaultTransportStateEvent;
import net.ihiroky.niotty.FailedTransportFuture;
import net.ihiroky.niotty.PipelineComposer;
import net.ihiroky.niotty.SucceededTransportFuture;
import net.ihiroky.niotty.Transport;
import net.ihiroky.niotty.TransportAggregate;
import net.ihiroky.niotty.TransportAggregateSupport;
import net.ihiroky.niotty.TransportFuture;
import net.ihiroky.niotty.TransportParameter;
import net.ihiroky.niotty.TransportState;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Set;

/**
 * Created on 13/01/10, 14:38
 *
 * @author Hiroki Itoh
 */
public class NioServerSocketTransport extends NioSocketTransport<AcceptSelector> implements TransportAggregate {

    private ServerSocketChannel serverChannel_;
    private NioServerSocketProcessor processor_;
    private NioServerSocketConfig config_;
    private TransportAggregateSupport childAggregate_;

    NioServerSocketTransport(NioServerSocketConfig config, NioServerSocketProcessor processor) {
        super(processor.name(), PipelineComposer.empty());

        ServerSocketChannel serverChannel = null;
        try {
            serverChannel = ServerSocketChannel.open();
            serverChannel.configureBlocking(false);
            config.applySocketOptions(serverChannel);

            this.config_ = config;
            this.serverChannel_ = serverChannel;
            this.processor_ = processor;
            this.childAggregate_ = new TransportAggregateSupport();
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
            serverChannel_.bind(socketAddress, config_.getBacklog());
            processor_.getAcceptSelectorPool().register(serverChannel_, SelectionKey.OP_ACCEPT, this);
            return new SucceededTransportFuture(this);
        } catch (IOException ioe) {
            return new FailedTransportFuture(this, ioe);
        }
    }

    @Override
    public TransportFuture connect(SocketAddress remoteAddress) {
        throw new UnsupportedOperationException("connect");
    }

    @Override
    public TransportFuture close() {
        if (eventLoop() != null) {
            return closeSelectableChannel();
        }
        try {
            serverChannel_.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        return new SucceededTransportFuture(this);
    }

    void registerReadLater(SelectableChannel channel) throws IOException {
        // SocketChannel#getRemoteAddress() may throw IOException, so get remoteAddress first.
        InetSocketAddress remoteAddress = (InetSocketAddress) ((SocketChannel) channel).getRemoteAddress();

        NioClientSocketTransport child = new NioClientSocketTransport(
                config_, processor_.pipelineComposer(), processor_.name(), (SocketChannel) channel);
        child.loadEvent(new DefaultTransportStateEvent(TransportState.CONNECTED, remoteAddress));
        processor_.getMessageIOSelectorPool().register(channel, SelectionKey.OP_READ, child);
        childAggregate_.add(child);

        transportListener().onAccept(child, remoteAddress);
    }

    public Set<Transport> childSet() {
        return childAggregate_.childSet();
    }
}
