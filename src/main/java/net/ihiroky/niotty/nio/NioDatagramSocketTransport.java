package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.DefaultTransportFuture;
import net.ihiroky.niotty.PipelineComposer;
import net.ihiroky.niotty.TaskLoop;
import net.ihiroky.niotty.TransportFuture;
import net.ihiroky.niotty.TransportState;
import net.ihiroky.niotty.TransportStateEvent;
import net.ihiroky.niotty.buffer.BufferSink;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.util.Objects;

/**
 * @author Hiroki Itoh
 */
public class NioDatagramSocketTransport extends NioSocketTransport<TcpIOSelector> {

    private DatagramChannel channel_;
    private WriteQueue writeQueue_;

    NioDatagramSocketTransport(NioDatagramSocketConfig config, PipelineComposer composer,
                               String name, UdpIOSelectorPool selectorPool) {
        Objects.requireNonNull(composer, "composer");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(selectorPool, "selectorPool");

        DatagramChannel channel = null;
        try {
            channel = DatagramChannel.open();
            config.applySocketOptions(channel);
            channel.configureBlocking(false);
        } catch (IOException ioe) {
            if (channel != null) {
                try {
                    channel.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            throw new RuntimeException("failed to open DatagramChannel.", ioe);
        }

        setUpPipelines(name, composer);

        channel_ = channel;
        writeQueue_ = config.newWriteQueue();

        // TODO attach a thread for remote ip from a pool.
        // TODO set read buffer size to 64k.
        selectorPool.register(channel, SelectionKey.OP_READ, this);
    }

    @Override
    public TransportFuture close() {
        return closeSelectableChannel(TransportState.CONNECTED);
    }

    @Override
    public void bind(SocketAddress local) throws IOException {
        channel_.bind(local);
    }

    @Override
    public TransportFuture connect(final SocketAddress remote) {
        final DefaultTransportFuture future = new DefaultTransportFuture(this);
        executeStore(new TransportStateEvent(TransportState.CONNECTED) {
            @Override
            public void execute() {
                try {
                    channel_.connect(remote);
                    getTransportListener().onConnect(NioDatagramSocketTransport.this, remote);
                } catch (IOException ioe) {
                    future.setThrowable(ioe);
                }
                future.done();
            }
        });
        return future;
    }

    // TODO membership management
    @Override
    public void join(InetAddress group, NetworkInterface networkInterface) throws IOException {
        channel_.join(group, networkInterface);
    }

    @Override
    public void join(InetAddress group, NetworkInterface networkInterface, InetAddress source) throws IOException {
        channel_.join(group, networkInterface, source);
    }

    @Override
    public void write(Object message) {
        executeStore(message);
    }

    @Override
    protected void writeDirect(final BufferSink buffer) {
        if (isInLoopThread()) {
            writeBufferSink(buffer);
        } else {
            offerTask(new TaskLoop.Task<TcpIOSelector>() {
                @Override
                public int execute(TcpIOSelector eventLoop) throws Exception {
                    writeBufferSink(buffer);
                    return TaskLoop.TIMEOUT_NO_LIMIT;
                }
            });
        }
    }

    @Override
    public SocketAddress localAddress() {
        try {
            return channel_.getLocalAddress();
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    @Override
    public SocketAddress remoteAddress() {
        try {
            return channel_.getRemoteAddress();
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    @Override
    public boolean isOpen() {
        return channel_.isOpen();
    }

    public boolean isConnected() {
        return channel_.isConnected();
    }

    void writeBufferSink(BufferSink buffer) {
        writeQueue_.offer(buffer);
    }

    int flush(ByteBuffer writeBuffer) throws IOException {
        return writeQueue_.flushTo(channel_, writeBuffer).waitTimeMillis_;
    }

    @Override
    void onCloseSelectableChannel() {
        writeQueue_.clear();
    }
}
