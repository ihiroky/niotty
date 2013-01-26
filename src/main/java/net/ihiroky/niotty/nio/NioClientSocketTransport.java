package net.ihiroky.niotty.nio;

import java.io.IOException;
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
    private ConnectSelectorPool connectSelectorPool;
    private MessageIOSelectorPool messageIOSelectorPool;
    private NioChildChannelTransport<MessageIOSelector> childTransport;

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
    public void bind(SocketAddress localAddress) {
        try {
            clientChannel.bind(localAddress);
        } catch (IOException ioe) {
            throw new RuntimeException("failed to bind " + clientChannel + " to " + localAddress, ioe);
        }
    }

    @Override
    public void connect(SocketAddress remoteAddress) {
        try {
            clientChannel.connect(remoteAddress);
            connectSelectorPool.register(this, clientChannel, SelectionKey.OP_CONNECT);
        } catch (IOException ioe) {
            throw new RuntimeException("failed to connect " + clientChannel + " to " + remoteAddress, ioe);
        }
    }

    @Override
    public void close() {
        if (getSelector() != null) {
            closeLater();
        } else {
            closeSelectableChannel();
        }
        if (childTransport != null) {
            childTransport.closeLater();
        }
    }

    @Override
    public void write(final Object message) {

    }

    NioChildChannelTransport<MessageIOSelector> registerLater(SelectableChannel channel, int ops) {
        NioChildChannelTransport<MessageIOSelector> child = new NioChildChannelTransport<MessageIOSelector>();
        this.childTransport = child;
        messageIOSelectorPool.register(child, channel, ops);
        return childTransport;
    }
}
