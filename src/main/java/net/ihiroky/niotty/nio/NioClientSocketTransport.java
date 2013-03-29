package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.DefaultTransportFuture;
import net.ihiroky.niotty.FailedTransportFuture;
import net.ihiroky.niotty.SucceededTransportFuture;
import net.ihiroky.niotty.TaskLoop;
import net.ihiroky.niotty.TransportFuture;
import net.ihiroky.niotty.TransportState;
import net.ihiroky.niotty.buffer.BufferSink;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Objects;

/**
 * @author Hiroki Itoh
 */
public class NioClientSocketTransport extends NioSocketTransport<MessageIOSelector> {

    private final SocketChannel clientChannel_;
    private final ConnectSelectorPool connector_;
    private final WriteQueue writeQueue_;

    NioClientSocketTransport(NioClientSocketConfig config, String name, ConnectSelectorPool connector) {
        Objects.requireNonNull(config, "config");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(connector, "connector");

        try {
            SocketChannel clientChannel = SocketChannel.open();
            clientChannel.configureBlocking(false);
            config.applySocketOptions(clientChannel);

            setUpPipelines(name, config.getPipelineInitializer());

            clientChannel_ = clientChannel;
            connector_ = connector;
            writeQueue_ = config.newWriteQueue();
        } catch (Exception e) {
            throw new RuntimeException("failed to open client socket channel.", e);
        }
    }

    NioClientSocketTransport(NioServerSocketConfig config, String name, SocketChannel child) {
        Objects.requireNonNull(config, "config");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(child, "child");

        setUpPipelines(name, config.getPipelineInitializer());

        clientChannel_ = child;
        connector_ = null;
        writeQueue_ = config.newWriteQueue();
    }

    @Override
    protected void writeDirect(final BufferSink buffer) {
        if (isInLoopThread()) {
            writeBufferSink(buffer);
        } else {
            offerTask(new TaskLoop.Task<MessageIOSelector>() {
                @Override
                public boolean execute(MessageIOSelector eventLoop) throws Exception {
                    writeBufferSink(buffer);
                    return true;
                }
            });
        }
    }

    @Override
    public void bind(SocketAddress local) throws IOException {
        if (connector_ == null) {
            throw new IllegalStateException("Channel is an accepted channel.");
        }
        clientChannel_.bind(local);
    }

    @Override
    public TransportFuture connect(SocketAddress remote) {
        if (connector_ == null) {
            throw new IllegalStateException("Channel is an accepted channel.");
        }
        try {
            if (clientChannel_.connect(remote)) {
                return new SucceededTransportFuture(this);
            }
            DefaultTransportFuture future = new DefaultTransportFuture(this);
            connector_.register(clientChannel_, SelectionKey.OP_CONNECT, new ConnectionWaitTransport(this, future));
            return future;
        } catch (IOException ioe) {
            return new FailedTransportFuture(this, ioe);
        }
    }

    @Override
    public TransportFuture close() {
        return isInLoopThread() ? closeSelectableChannel() : closeSelectableChannelLater(TransportState.CONNECTED);
    }

    @Override
    public void join(InetAddress group, NetworkInterface networkInterface) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void join(InetAddress group, NetworkInterface networkInterface, InetAddress source) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void write(Object message) {
        executeStore(message);
    }

    @Override
    public void write(Object message, SocketAddress remote) {
        throw new UnsupportedOperationException();
    }

    @Override
    public InetSocketAddress localAddress() {
        try {
            return (InetSocketAddress) clientChannel_.getLocalAddress();
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    @Override
    public InetSocketAddress remoteAddress() {
        try {
            return (InetSocketAddress) clientChannel_.getRemoteAddress();
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    @Override
    public boolean isOpen() {
        return clientChannel_.isOpen();
    }

    public boolean isConnected() {
        return clientChannel_.isConnected();
    }

    void writeBufferSink(BufferSink buffer) {
        writeQueue_.offer(buffer);
    }

    boolean flush(ByteBuffer byteBuffer) throws IOException {
        WritableByteChannel channel = (WritableByteChannel) getSelectionKey().channel();
        WriteQueue.FlushStatus status = writeQueue_.flushTo(channel, byteBuffer);
        return status == WriteQueue.FlushStatus.FLUSHED;
    }

    void loadEvent(Object message) {
        executeLoad(message);
    }

    void fireOnConnect() {
        getTransportListener().onConnect(this, remoteAddress());
    }
}
