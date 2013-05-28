package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.AttachedMessage;
import net.ihiroky.niotty.DefaultTransportFuture;
import net.ihiroky.niotty.DefaultTransportParameter;
import net.ihiroky.niotty.PipelineComposer;
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
public class NioDatagramSocketTransport extends NioSocketTransport<UdpIOSelector> {

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

    /**
     * <p>Connects this transport's socket.</p>
     *
     * <p>The socket is configured so that it only receives datagrams from, and sends datagrams to,
     * the given remote peer address. Once connected, datagrams may not be received from or sent to any other address.
     * A datagram socket remains connected until it is explicitly disconnected or until it is closed.</p>
     *
     * <p>This method is asynchronously invoked. Use a future object returned to confirm the operation is
     * correctly done or not.</p>
     *
     * @param remote The remote address to which this channel is to be connected.
     * @return The future object.
     */
    @Override
    public TransportFuture connect(final SocketAddress remote) {
        final DefaultTransportFuture future = new DefaultTransportFuture(this);
        executeStore(new TransportStateEvent(TransportState.CONNECTED) {
            @Override
            public void execute() {
                try {
                    channel_.connect(remote);
                    getTransportListener().onConnect(NioDatagramSocketTransport.this, remote);
                    future.done();
                } catch (IOException ioe) {
                    future.setThrowable(ioe);
                }
            }
        });
        return future;
    }

    /**
     * <p>Disconnects this transport's socket.</p>
     *
     * <p>The socket is configured so that it can receive datagrams from, and sends datagrams to,
     * any remote address so long as the security manager, if installed, permits it.</p>
     *
     * <p>This method may be invoked at any time. It will not have any effect on read or write operations
     * that are already in progress at the moment that it is invoked.</p>
     *
     * <p>If the socket is not connected, or if the transport is closed, then invoking this method has no effect.</p>
     *
     * <p>This method is asynchronously invoked. Use a future object returned to confirm the operation is
     * correctly done or not.</p>

     * @return The future object.
     */
    public TransportFuture disconnect() {
        final DefaultTransportFuture future = new DefaultTransportFuture(this);
        executeStore(new TransportStateEvent(TransportState.CONNECTED) {
            @Override
            public void execute() {
                try {
                    channel_.disconnect();
                    getTransportListener().onConnect(NioDatagramSocketTransport.this, null);
                    future.done();
                } catch (IOException ioe) {
                    future.setThrowable(ioe);
                }
            }
        });
        return future;
    }

    public void write(Object message, SocketAddress target) {
        super.write(message, new DefaultTransportParameter(target));
    }

    public void write(Object message, int priority, SocketAddress target) {
        super.write(message, new DefaultTransportParameter(priority, target));
    }

    // TODO membership management
    public void join(InetAddress group, NetworkInterface networkInterface) throws IOException {
        channel_.join(group, networkInterface);
    }

    public void join(InetAddress group, NetworkInterface networkInterface, InetAddress source) throws IOException {
        channel_.join(group, networkInterface, source);
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

    void readyToWrite(AttachedMessage<BufferSink> message) {
        writeQueue_.offer(message);
    }

    int flush(ByteBuffer writeBuffer) throws IOException {
        return writeQueue_.flushTo(channel_, writeBuffer).waitTimeMillis_;
    }

    @Override
    void onCloseSelectableChannel() {
        writeQueue_.clear();
    }
}
