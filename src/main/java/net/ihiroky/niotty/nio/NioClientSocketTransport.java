package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.EventLoop;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
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
    private ConnectSelectorPool connectSelectorPool;
    private MessageIOSelectorPool messageIOSelectorPool;
    private volatile NioChildChannelTransport childTransport;

    public NioClientSocketTransport(NioClientSocketConfig cfg,
                                    ConnectSelectorPool connectPool, MessageIOSelectorPool messageIOPool) {
        try {
            clientChannel = SocketChannel.open();
            clientChannel.configureBlocking(false);
            config = cfg;
            connectSelectorPool = connectPool;
            messageIOSelectorPool = messageIOPool;
            cfg.applySocketOptions(clientChannel.socket());
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
            connectSelectorPool.register(this, clientChannel, SelectionKey.OP_CONNECT);
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

    NioChildChannelTransport registerLater(SelectableChannel channel, int ops) {
        try {
            getTransportListener().onConnect(this, clientChannel.getRemoteAddress());
        } catch (IOException ignored) {
        }
        NioChildChannelTransport child = new NioChildChannelTransport(config);
        this.childTransport = child;
        messageIOSelectorPool.register(child, channel, ops);
        return childTransport;
    }

    @Override
    protected void writeDirect(final ByteBuffer byteBuffer) {
        getEventLoop().offerTask(new EventLoop.Task<ConnectSelector>() {
            @Override
            public boolean execute(ConnectSelector eventLoop) {
                childTransport.readyToWrite(byteBuffer);
                return true;
            }
        });
    }
}
