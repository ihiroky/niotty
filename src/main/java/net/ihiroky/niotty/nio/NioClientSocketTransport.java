package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.PipeLine;
import net.ihiroky.niotty.StageContext;
import net.ihiroky.niotty.StageContextAdapter;
import net.ihiroky.niotty.Transport;
import net.ihiroky.niotty.event.MessageEvent;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
    private PipeLine multicastStorePipeLine;
    private ExecutorService multicastStorePipeLineExecutor;
    private volatile boolean connected;



    public NioClientSocketTransport(NioClientSocketConfig cfg,
                                    ConnectSelectorPool connectPool, MessageIOSelectorPool messageIOPool) {
        try {
            clientChannel = SocketChannel.open();
            clientChannel.configureBlocking(false);
            config = cfg;
            connectSelectorPool = connectPool;
            messageIOSelectorPool = messageIOPool;
            cfg.applySocketOptions(clientChannel.socket());
            multicastStorePipeLine = cfg.getStorePipeLineFactory().createPipeLine();
            multicastStorePipeLine.getLastContext().addListener(new StageContextAdapter<ByteBuffer>() {
                @Override
                public void onProceed(PipeLine pipeLine, StageContext context, MessageEvent<ByteBuffer> event) {
                    // executed in I/O threads.
                    messageIOSelectorPool.offerTask(
                            new MessageIOSelector.BroadcastTask(event.getMessage()));
                }
            });
            multicastStorePipeLineExecutor = Executors.newSingleThreadExecutor();
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
        multicastStorePipeLineExecutor.shutdownNow();
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
        multicastStorePipeLineExecutor.execute(new Runnable() {
            @Override
            public void run() {
                multicastStorePipeLine.fire(new MessageEvent<Object>(NioClientSocketTransport.this, message));
            }
        });
    }

    @Override
    public Transport getParent() {
        return this;
    }

    NioChildChannelTransport<MessageIOSelector> registerLater(SelectableChannel channel, int ops) {
        NioChildChannelTransport<MessageIOSelector> child = new NioChildChannelTransport<MessageIOSelector>(this);
        this.childTransport = child;
        messageIOSelectorPool.register(child, channel, ops);
        return childTransport;
    }
}
