package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.DefaultTransportFuture;
import net.ihiroky.niotty.DefaultTransportParameter;
import net.ihiroky.niotty.FailedTransportFuture;
import net.ihiroky.niotty.PipelineComposer;
import net.ihiroky.niotty.SucceededTransportFuture;
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
import java.nio.channels.MembershipKey;
import java.nio.channels.SelectionKey;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * A Transport implementation for {@code java.nio.channels.DatagramChannel}.
 *
 * @author Hiroki Itoh
 */
public class NioDatagramSocketTransport extends NioSocketTransport<UdpIOSelector> {

    private DatagramChannel channel_;
    private WriteQueue writeQueue_;
    private final Map<GroupKey, MembershipKey> membershipKeyMap_;

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
        membershipKeyMap_ = Collections.synchronizedMap(new HashMap<GroupKey, MembershipKey>());

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

    /**
     * <p>Writes the message to the {@code DatagramChannel} via a pipeline associated with this transport.
     * The message is sent to the given target. If this transport is connected
     * by {@link #connect(java.net.SocketAddress)}, an invocation of this method is failed.</p>
     *
     * @param message The message to be sent.
     * @param target The target to which the message is sent.
     */
    public void write(Object message, SocketAddress target) {
        super.write(message, new DefaultTransportParameter(target));
    }

    /**
     * <p>Writes the message to the {@code DatagramChannel} via a pipeline associated with this transport.
     * The message is sent to the given target. If this transport is connected
     * by {@link #connect(java.net.SocketAddress)}, an invocation of this method is failed.</p>
     *
     * @param message The message to be sent.
     * @param priority A priority which is used in {@link net.ihiroky.niotty.nio.WriteQueue}
     * @param target The target to which the message is sent.
     */
    public void write(Object message, int priority, SocketAddress target) {
        super.write(message, new DefaultTransportParameter(priority, target));
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

    /**
     * Returns true if this transport is connected.
     * @return true if this transport is connected.
     */
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

    /**
     * <p>Joins a multicast group to begin receiving all datagrams sent to the group on the given network interface.</p>
     *
     * @param group The multicast group address.
     * @param networkInterface The network interface on which to join the group.
     * @return A future object to show a result.
     */
    public TransportFuture joinGroup(InetAddress group, NetworkInterface networkInterface) {
        final GroupKey key = new GroupKey(group, networkInterface);
        if (membershipKeyMap_.containsKey(key)) {
            return new SucceededTransportFuture(this);
        }

        try {
            MembershipKey membershipKey = channel_.join(key.group_, key.networkInterface_);
            membershipKeyMap_.put(key, membershipKey);
            return new SucceededTransportFuture(this);
        } catch (Exception e) {
            return new FailedTransportFuture(this, e);
        }
    }

    /**
     * <p>Joins a multicast group to begin receiving datagrams sent to the group from a given source address.
     * on the given network interface.</p>
     *
     * @param group The multicast group address.
     * @param networkInterface The network interface on which to join the group.
     * @param source The source address from which datagrams is sent.
     * @return A future object to show a result of this method.
     */
    public TransportFuture joinGroup(InetAddress group, NetworkInterface networkInterface, final InetAddress source) {
        final GroupKey key = new GroupKey(group, networkInterface);
        if (membershipKeyMap_.containsKey(key)) {
            return new SucceededTransportFuture(this);
        }

        try {
            MembershipKey membershipKey = channel_.join(key.group_, key.networkInterface_, source);
            membershipKeyMap_.put(key, membershipKey);
            return new SucceededTransportFuture(this);
        } catch (Exception e) {
            return new FailedTransportFuture(this, e);
        }
    }

    /**
     * <p>Drop membership.</p>
     *
     * <p>This transport will no longer receive any datagrams sent to the group if the membership is dropped.</p>
     *
     * @param group The group address.
     * @param networkInterface The network interface
     * @return A future object to show a result of this method.
     */
    public TransportFuture leaveGroup(InetAddress group, NetworkInterface networkInterface) {
        final GroupKey key = new GroupKey(group, networkInterface);
        if (!membershipKeyMap_.containsKey(key)) {
            return new SucceededTransportFuture(this);
        }

        final DefaultTransportFuture future = new DefaultTransportFuture(this);
        MembershipKey membershipKey = membershipKeyMap_.remove(key);
        membershipKey.drop();
        return new SucceededTransportFuture(this);
    }

    /**
     * <p>Blocks multicast datagrams from the given source address.</p>
     *
     * @param group The group address.
     * @param networkInterface The network interface on which to join the group.
     * @param source The source address to block.
     * @return A future object to show a result of this method.
     */
    public TransportFuture block(InetAddress group, NetworkInterface networkInterface, final InetAddress source) {
        GroupKey key = new GroupKey(group, networkInterface);
        final MembershipKey membershipKey = membershipKeyMap_.get(key);
        if (membershipKey == null) {
            return new SucceededTransportFuture(this);
        }

        try {
            membershipKey.block(source);
            return new SucceededTransportFuture(this);
        } catch (Exception e) {
            return new FailedTransportFuture(this, e);
        }
    }

    /**
     * <p>Unblock multicast datagrams from the given source address that was previously blocked.</p>
     *
     * @param group The group address.
     * @param networkInterface The network interface on which to join the group.
     * @param source The source address to unblock.
     * @return A future object to show a result of this method.
     */
    public TransportFuture unblock(InetAddress group, NetworkInterface networkInterface, final InetAddress source) {
        GroupKey key = new GroupKey(group, networkInterface);
        final MembershipKey membershipKey = membershipKeyMap_.get(key);
        if (membershipKey == null) {
            return new SucceededTransportFuture(this);
        }

        try {
            membershipKey.unblock(source);
            return new SucceededTransportFuture(this);
        } catch (Exception e) {
            return new FailedTransportFuture(this, e);
        }
    }

    private class GroupKey {
        final InetAddress group_;
        final NetworkInterface networkInterface_;

        GroupKey(InetAddress group, NetworkInterface networkInterface) {
            group_ = group;
            networkInterface_ = networkInterface;
        }

        private static final int BASE = 17;
        private static final int FACTOR = 31;

        @Override
        public int hashCode() {
            int h = BASE;
            h = h * FACTOR + ((group_ != null) ? group_.hashCode() : 0);
            h = h * FACTOR + ((networkInterface_ != null) ? networkInterface_.hashCode() : 0);
            return h;
        }

        @Override
        public boolean equals(Object object) {
            if (object instanceof GroupKey) {
                GroupKey that = (GroupKey) object;
                boolean groupEqual = (this.group_ != null)
                        ? this.group_.equals(that.group_) : that.group_ == null;
                boolean networkInterfaceEqual = (this.networkInterface_ != null)
                        ? this.networkInterface_.equals(that.networkInterface_) : that.networkInterface_ == null;
                return groupEqual && networkInterfaceEqual;
            }
            return false;
        }
    }
}
