package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.EventLoop;
import net.ihiroky.niotty.buffer.BufferSink;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

/**
 * Created on 13/01/17, 16:13
 *
 * @author Hiroki Itoh
 */
public class NioClientSocketTransport extends NioSocketTransport<ConnectSelector> {

    private SocketChannel clientChannel;
    private NioClientSocketConfig config;
    private NioClientSocketProcessor processor;
    private volatile NioChildChannelTransport childTransport;

    public NioClientSocketTransport(NioClientSocketConfig config, NioClientSocketProcessor processor) {
        try {

            SocketChannel clientChannel = SocketChannel.open();
            clientChannel.configureBlocking(false);
            config.applySocketOptions(clientChannel);

            this.clientChannel = clientChannel;
            this.config = config;
            this.processor = processor;
        } catch (Exception e) {
            throw new RuntimeException("failed to open client socket channel.", e);
        }
    }

    @Override
    public void bind(SocketAddress local) {
        try {
            clientChannel.bind(local);
            getTransportListener().onBind(this, local);
        } catch (IOException ioe) {
            throw new RuntimeException("failed to bind " + clientChannel + " to " + local, ioe);
        }
    }

    @Override
    public void connect(SocketAddress remote) {
        try {
            clientChannel.connect(remote);
            processor.getConnectSelectorPool().register(this, clientChannel, SelectionKey.OP_CONNECT);
        } catch (IOException ioe) {
            throw new RuntimeException("failed to connect " + clientChannel + " to " + remote, ioe);
        }
    }

    @Override
    public void close() {
        if (getEventLoop() != null) {
            closeLater();
        } else {
            closeSelectableChannel();
        }
        if (childTransport != null) {
            // onClose() is called by childTransport.
            childTransport.closeLater();
        }
    }

    @Override
    public void join(InetAddress group, NetworkInterface networkInterface, InetAddress source) {
        throw new UnsupportedOperationException("join");
    }

    @Override
    public void write(final Object message) {
        NioChildChannelTransport transport = childTransport;
        if (transport == null) {
            throw new IllegalStateException("not connected.");
        }
        transport.write(message);
    }

    @Override
    public void write(Object message, SocketAddress remote) {
        throw new UnsupportedOperationException("write");
    }

    @Override
    public InetSocketAddress localAddress() {
        try {
            return (InetSocketAddress) clientChannel.getLocalAddress();
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    @Override
    public InetSocketAddress remoteAddress() {
        try {
            return (InetSocketAddress) clientChannel.getRemoteAddress();
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    @Override
    public boolean isOpen() {
        return clientChannel.isOpen();
    }

    NioChildChannelTransport registerLater(SelectableChannel channel, int ops) {
        try {
            getTransportListener().onConnect(this, clientChannel.getRemoteAddress());
        } catch (IOException ignored) {
        }
        NioChildChannelTransport child =
                new NioChildChannelTransport(config, processor.getWriteBufferSize(), processor.isUseDirectBuffer());
        this.childTransport = child;
        processor.getMessageIOSelectorPool().register(child, channel, ops);
        return childTransport;
    }

    @Override
    protected void writeDirect(final BufferSink buffer) {
        getEventLoop().offerTask(new EventLoop.Task<ConnectSelector>() {
            @Override
            public boolean execute(ConnectSelector eventLoop) throws Exception {
                childTransport.writeBufferSink(buffer);
                return true;
            }
        });
    }
}
