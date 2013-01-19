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
import java.nio.channels.ServerSocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created on 13/01/10, 14:38
 *
 * @author Hiroki Itoh
 */
public class NioServerSocketTransport extends NioSocketTransport<AcceptSelector> {

    private ServerSocketChannel serverChannel;
    private AcceptSelectorPool acceptSelectorPool;
    private MessageIOSelectorPool messageIOSelectorPool;
    private NioServerSocketConfig config;
    private PipeLine multicastStorePipeLine;
    private ExecutorService multicastStorePipeLineExecutor;

    NioServerSocketTransport(NioServerSocketConfig cfg,
                             AcceptSelectorPool acceptPool,
                             MessageIOSelectorPool messageIOPool) {
        try {
            config = cfg;
            serverChannel = ServerSocketChannel.open();
            serverChannel.configureBlocking(false);
            cfg.applySocketOptions(serverChannel.socket());
            acceptSelectorPool = acceptPool;
            messageIOSelectorPool = messageIOPool;
            multicastStorePipeLine = config.getStorePipeLineFactory().createPipeLine();
            multicastStorePipeLine.getLastContext().addListener(new StageContextAdapter<ByteBuffer>() {
                @Override
                public void onProceed(PipeLine pipeLine, StageContext context, MessageEvent<ByteBuffer> event) {
                    // executed in I/O threads.
                    messageIOSelectorPool.offerTask(
                            new MessageIOSelector.BroadcastTask(event.getMessage()));
                }
            });
            multicastStorePipeLineExecutor = Executors.newSingleThreadExecutor();
        } catch (IOException ioe) {
            if (serverChannel != null) {
                try {
                    serverChannel.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (messageIOSelectorPool != null) {
                messageIOSelectorPool.close();
            }
            throw new RuntimeException("failed to open NioServerSocketTransport.", ioe);
        }
    }

    @Override
    public void write(final Object message) {
        multicastStorePipeLineExecutor.execute(new Runnable() {
            @Override
            public void run() {
                multicastStorePipeLine.fire(new MessageEvent<Object>(NioServerSocketTransport.this, message));
            }
        });
    }

    @Override
    public Transport getParent() {
        return this;
    }

    @Override
    public void bind(SocketAddress socketAddress) {
        try {
            serverChannel.bind(socketAddress, config.getBacklog());
            acceptSelectorPool.register(this, serverChannel, SelectionKey.OP_ACCEPT);
        } catch (IOException e) {
            throw new RuntimeException("failed to bind server socket:" + socketAddress, e);
        }
    }

    @Override
    public void connect(SocketAddress remoteAddress) {
        throw new UnsupportedOperationException("connect");
    }

    @Override
    public void close() {
        multicastStorePipeLineExecutor.shutdownNow();
        closeLater();
        messageIOSelectorPool.close();
    }

    NioChildChannelTransport<MessageIOSelector> registerLater(SelectableChannel channel, int ops) {
        NioChildChannelTransport<MessageIOSelector> child = new NioChildChannelTransport<MessageIOSelector>(this);
        messageIOSelectorPool.register(child, channel, ops);
        return child;
    }
}
