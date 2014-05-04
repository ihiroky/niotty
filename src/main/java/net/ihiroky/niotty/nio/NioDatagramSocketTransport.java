package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.DefaultTransportFuture;
import net.ihiroky.niotty.Event;
import net.ihiroky.niotty.EventDispatcherGroup;
import net.ihiroky.niotty.FailedTransportFuture;
import net.ihiroky.niotty.PipelineComposer;
import net.ihiroky.niotty.SuccessfulTransportFuture;
import net.ihiroky.niotty.TransportException;
import net.ihiroky.niotty.TransportFuture;
import net.ihiroky.niotty.TransportOption;
import net.ihiroky.niotty.TransportOptions;
import net.ihiroky.niotty.buffer.Buffers;
import net.ihiroky.niotty.buffer.Packet;
import net.ihiroky.niotty.util.Arguments;
import net.ihiroky.niotty.util.JavaVersion;
import net.ihiroky.niotty.util.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.MembershipKey;
import java.nio.channels.SelectionKey;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * An implementation of {@link net.ihiroky.niotty.Transport} for NIO {@code DatagramChannel}.
 */
public class NioDatagramSocketTransport extends NioSocketTransport<SelectDispatcher> {

    private final DatagramChannel channel_;
    private final DatagramQueue writeQueue_;
    private FlushStatus flushStatus_;
    private final Map<GroupKey, MembershipKey> membershipKeyMap_;

    private static Logger logger_ = LoggerFactory.getLogger(NioDatagramSocketTransport.class);

    private static final Set<TransportOption<?>> SUPPORTED_OPTIONS = Collections.unmodifiableSet(
            new HashSet<TransportOption<?>>(Arrays.<TransportOption<?>>asList(
                    TransportOptions.SO_RCVBUF, TransportOptions.SO_SNDBUF, TransportOptions.SO_BROADCAST,
                    TransportOptions.SO_REUSEADDR,
                    TransportOptions.IP_MULTICAST_IF, TransportOptions.IP_MULTICAST_LOOP,
                    TransportOptions.IP_MULTICAST_TTL, TransportOptions.IP_TOS)));

    /**
     * Constructs the instance.
     * @param name the name of this transport
     * @param composer the composer to initialize a pipeline for this transport
     * @param ioSelectDispatcherGroup the pool which offers the SelectDispatcher to execute the stage
     * @param writeQueueFactory the factory to create DatagramQueue
     * @param family the internet protocol family
     */
    public NioDatagramSocketTransport(String name, PipelineComposer composer,
            EventDispatcherGroup<SelectDispatcher> ioSelectDispatcherGroup,
            WriteQueueFactory<DatagramQueue> writeQueueFactory,
            InternetProtocolFamily family) {
        super(name, composer, ioSelectDispatcherGroup);

        Arguments.requireNonNull(writeQueueFactory, "writeQueueFactory");

        DatagramChannel channel = null;
        try {
            channel = (family != null)
                    ? DatagramChannel.open(InternetProtocolFamily.resolve(family))
                    : DatagramChannel.open();
            channel.configureBlocking(false);
            register(channel, SelectionKey.OP_READ);
        } catch (IOException ioe) {
            if (channel != null) {
                try {
                    channel.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            throw new TransportException("Failed to open DatagramChannel.", ioe);
        }

        channel_ = channel;
        writeQueue_ = writeQueueFactory.newWriteQueue();
        membershipKeyMap_ = Collections.synchronizedMap(new HashMap<GroupKey, MembershipKey>());
    }

    // Constructor for the test.
    NioDatagramSocketTransport(String name, PipelineComposer composer,
            EventDispatcherGroup<SelectDispatcher> ioSelectDispatcherGroup,
            WriteQueueFactory<DatagramQueue> writeQueueFactory,
            DatagramChannel channel) {
        super(name, composer, ioSelectDispatcherGroup);

        Arguments.requireNonNull(writeQueueFactory, "writeQueueFactory");

        channel_ = channel;
        writeQueue_ = writeQueueFactory.newWriteQueue();
        membershipKeyMap_ = Collections.synchronizedMap(new HashMap<GroupKey, MembershipKey>());
    }

    /**
     * Sets a socket option.
     * @param option the option
     * @param value the value of the option
     * @param <T> the type of the option
     * @return this object
     * @throws net.ihiroky.niotty.TransportException if an I/O error occurs
     * @throws java.lang.UnsupportedOperationException if the option is not supported
     */
    @Override
    public <T> NioDatagramSocketTransport setOption(TransportOption<T> option, T value) {
        try {
            JavaVersion javaVersion = Platform.javaVersion();
            if (javaVersion.ge(JavaVersion.JAVA7)) {
                if (option == TransportOptions.SO_RCVBUF) {
                    channel_.setOption(StandardSocketOptions.SO_RCVBUF, (Integer) value);
                } else if (option == TransportOptions.SO_SNDBUF) {
                    channel_.setOption(StandardSocketOptions.SO_SNDBUF, (Integer) value);
                } else if (option == TransportOptions.SO_BROADCAST) {
                    channel_.setOption(StandardSocketOptions.SO_BROADCAST, (Boolean) value);
                } else if (option == TransportOptions.SO_REUSEADDR) {
                    channel_.setOption(StandardSocketOptions.SO_REUSEADDR, (Boolean) value);
                } else if (option == TransportOptions.IP_MULTICAST_IF) {
                    channel_.setOption(StandardSocketOptions.IP_MULTICAST_IF, (NetworkInterface) value);
                } else if (option == TransportOptions.IP_MULTICAST_LOOP) {
                    channel_.setOption(StandardSocketOptions.IP_MULTICAST_LOOP, (Boolean) value);
                } else if (option == TransportOptions.IP_MULTICAST_TTL) {
                    channel_.setOption(StandardSocketOptions.IP_MULTICAST_TTL, (Integer) value);
                } else if (option == TransportOptions.IP_TOS) {
                    channel_.setOption(StandardSocketOptions.IP_TOS, (Integer) value);
                } else {
                    throw new UnsupportedOperationException(option.toString());
                }
            } else {
                if (option == TransportOptions.SO_RCVBUF) {
                    channel_.socket().setReceiveBufferSize((Integer) value);
                } else if (option == TransportOptions.SO_SNDBUF) {
                    channel_.socket().setSendBufferSize((Integer) value);
                } else if (option == TransportOptions.SO_BROADCAST) {
                    channel_.socket().setBroadcast((Boolean) value);
                } else if (option == TransportOptions.SO_REUSEADDR) {
                    channel_.socket().setReuseAddress((Boolean) value);
                } else if (SUPPORTED_OPTIONS.contains(option)) {
                    javaVersion.throwIfUnsupported(JavaVersion.JAVA7);
                } else {
                    throw new UnsupportedOperationException(option.toString());
                }
            }
        } catch (IOException ioe) {
            throw new TransportException("Failed to get option " + option, ioe);
        }
        return this;
    }

    /**
     * Returns a socket option value for a specified option.
     * @param option the option
     * @param <T> the type of the value
     * @return the value
     * @throws net.ihiroky.niotty.TransportException if I/O error occurs
     * @throws java.lang.UnsupportedOperationException if the option is not supported
     */
    @Override
    public <T> T option(TransportOption<T> option) {
        try {
            JavaVersion javaVersion = Platform.javaVersion();
            if (javaVersion.ge(JavaVersion.JAVA7)) {
                if (option == TransportOptions.SO_RCVBUF) {
                    return option.cast(channel_.getOption(StandardSocketOptions.SO_RCVBUF));
                } else if (option == TransportOptions.SO_SNDBUF) {
                    return option.cast(channel_.getOption(StandardSocketOptions.SO_SNDBUF));
                } else if (option == TransportOptions.SO_BROADCAST) {
                    return option.cast(channel_.getOption(StandardSocketOptions.SO_BROADCAST));
                } else if (option == TransportOptions.SO_REUSEADDR) {
                    return option.cast(channel_.getOption(StandardSocketOptions.SO_REUSEADDR));
                } else if (option == TransportOptions.IP_MULTICAST_IF) {
                    return option.cast(channel_.getOption(StandardSocketOptions.IP_MULTICAST_IF));
                } else if (option == TransportOptions.IP_MULTICAST_LOOP) {
                    return option.cast(channel_.getOption(StandardSocketOptions.IP_MULTICAST_LOOP));
                } else if (option == TransportOptions.IP_MULTICAST_TTL) {
                    return option.cast(channel_.getOption(StandardSocketOptions.IP_MULTICAST_TTL));
                } else if (option == TransportOptions.IP_TOS) {
                    return option.cast(channel_.getOption(StandardSocketOptions.IP_TOS));
                } else {
                    throw new UnsupportedOperationException(option.toString());
                }
            } else {
                if (option == TransportOptions.SO_RCVBUF) {
                    return option.cast(channel_.socket().getReceiveBufferSize());
                } else if (option == TransportOptions.SO_SNDBUF) {
                    return option.cast(channel_.socket().getSendBufferSize());
                } else if (option == TransportOptions.SO_BROADCAST) {
                    return option.cast(channel_.socket().getBroadcast());
                } else if (option == TransportOptions.SO_REUSEADDR) {
                    return option.cast(channel_.socket().getReuseAddress());
                } else if (SUPPORTED_OPTIONS.contains(option)) {
                    javaVersion.throwIfUnsupported(JavaVersion.JAVA7);
                    throw new AssertionError(); // must not reach here
                } else {
                    throw new UnsupportedOperationException(option.toString());
                }
            }
        } catch (IOException ioe) {
            throw new TransportException("Failed to get option " + option, ioe);
        }
    }

    @Override
    public Set<TransportOption<?>> supportedOptions() {
        return SUPPORTED_OPTIONS;
    }

    @Override
    public TransportFuture close() {
        return closeSelectableChannel();
    }

    /**
     * Binds the channel of this transport to the specified address and makes this transport readable.
     *
     * @param local The local address
     * @return a future object to get the result of this operation
     */
    @Override
    public TransportFuture bind(final SocketAddress local) {
        try {
            boolean bound = Platform.javaVersion().ge(JavaVersion.JAVA7)
                    ? (channel_.getLocalAddress() != null) : channel_.socket().isBound();
            if (bound) {
                return new SuccessfulTransportFuture(this);
            }
        } catch (IOException ioe) {
            return new FailedTransportFuture(this, ioe);
        }

        final DefaultTransportFuture future = new DefaultTransportFuture(this);
        eventDispatcher().execute(new Event() {;
            @Override
            public long execute() throws Exception {
                if (future.executing()) {
                    try {
                        DatagramChannel channel = channel_;
                        if (Platform.javaVersion().ge(JavaVersion.JAVA7)) {
                            if (channel.getLocalAddress() == null) {
                                channel.bind(local);
                            }
                        } else {
                            DatagramSocket socket = channel.socket();
                            if (!socket.isBound()) {
                                socket.bind(local);
                            }
                        }
                        future.done();
                    } catch (IOException ioe) {
                        future.setThrowable(ioe);
                    }
                }
                return DONE;
            }
        });
        return future;
    }

    @Override
    public InetSocketAddress localAddress() {
        try {
            return Platform.javaVersion().ge(JavaVersion.JAVA7)
                    ? (InetSocketAddress) channel_.getLocalAddress()
                    : (InetSocketAddress) channel_.socket().getLocalSocketAddress();
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    @Override
    public InetSocketAddress remoteAddress() {
        try {
            return Platform.javaVersion().ge(JavaVersion.JAVA7)
                    ? (InetSocketAddress) channel_.getRemoteAddress()
                    : (InetSocketAddress) channel_.socket().getRemoteSocketAddress();
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    @Override
    public boolean isOpen() {
        return channel_.isOpen();
    }

    @Override
    public int pendingWriteBuffers() {
        return writeQueue_.size();
    }

    /**
     * <p>Connects the socket of this transport.</p>
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
        eventDispatcher().execute(new Event() {
            @Override
            public long execute() throws Exception {
                if (future.executing()) {
                    try {
                        channel_.connect(remote);
                        future.done();
                    } catch (IOException ioe) {
                        future.setThrowable(ioe);
                    }
                }
                return DONE;
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
        eventDispatcher().execute(new Event() {
            @Override
            public long execute() {
                if (future.executing()) {
                    try {
                        channel_.disconnect();
                        future.done();
                    } catch (IOException ioe) {
                        future.setThrowable(ioe);
                    }
                }
                return DONE;
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
        super.write(message, target);
    }

    /**
     * Returns true if this transport is connected.
     * @return true if this transport is connected.
     */
    public boolean isConnected() {
        return channel_.isConnected();
    }

    @Override
    void readyToWrite(Packet message, Object parameter) {
        writeQueue_.offer(new AttachedMessage<Packet>(message, parameter));
    }

    @Override
    void onCloseSelectableChannel() {
        writeQueue_.clear();
    }

    @Override
    void onSelected(SelectionKey key, SelectDispatcher selectDispatcher) {
        assert key == key();

        DatagramChannel channel = (DatagramChannel) key.channel();
        try {
            if (key.isReadable()) {
                ByteBuffer readBuffer = selectDispatcher.readBuffer_;
                if (channel.isConnected()) {
                    int read = channel.read(readBuffer);
                    if (read == -1) {
                        logger_.debug("[onSelected] transport reaches the end of its stream: {}", this);
                        doCloseSelectableChannel();
                        return;
                    }
                    readBuffer.flip();
                    pipeline().load(Buffers.wrap(readBuffer), null);
                    readBuffer.clear();
                } else {
                    for (;;) {
                        SocketAddress source = channel.receive(readBuffer);
                        if (source == null) {
                            break;
                        }
                        readBuffer.flip();
                        pipeline().load(Buffers.wrap(readBuffer), source);
                        readBuffer.clear();
                    }
                }
                readBuffer.clear();
            } else if (key.isWritable()) {
                flushForcibly(selectDispatcher.writeBuffer_);
            }
        } catch (ClosedByInterruptException ie) {
            if (logger_.isDebugEnabled()) {
                logger_.debug("failed to read from transport by interruption:" + this, ie);
            }
            doCloseSelectableChannel();
        } catch (IOException ioe) {
            logger_.error("failed to read from transport:" + this, ioe);
            doCloseSelectableChannel();
        }
    }

    @Override
    void flush(ByteBuffer writeBuffer) throws IOException {
        if (flushStatus_ == FlushStatus.FLUSHING) {
            return;
        }

        flushForcibly(writeBuffer);
    }

    private void flushForcibly(ByteBuffer writeBuffer) throws IOException {
        FlushStatus status = writeQueue_.flush(channel_, writeBuffer);
        flushStatus_ = status;
        handleFlushStatus(status);
    }

    /**
     * <p>Joins a multicast group to begin receiving all datagrams sent to the group on the given network interface.</p>
     *
     * @param group The multicast group address.
     * @param networkInterface The network interface on which to join the group.
     * @return A future object to show a result.
     */
    public TransportFuture join(InetAddress group, NetworkInterface networkInterface) {
        Platform.javaVersion().throwIfUnsupported(JavaVersion.JAVA7);

        final GroupKey key = new GroupKey(group, networkInterface);
        synchronized (membershipKeyMap_) {
            if (membershipKeyMap_.containsKey(key)) {
                return new SuccessfulTransportFuture(this);
            }

            try {
                MembershipKey membershipKey = channel_.join(key.group_, key.networkInterface_);
                membershipKeyMap_.put(key, membershipKey);
                return new SuccessfulTransportFuture(this);
            } catch (Exception e) {
                return new FailedTransportFuture(this, e);
            }
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
    public TransportFuture join(InetAddress group, NetworkInterface networkInterface, final InetAddress source) {
        Platform.javaVersion().throwIfUnsupported(JavaVersion.JAVA7);

        final GroupKey key = new GroupKey(group, networkInterface);
        synchronized (membershipKeyMap_) {
            if (membershipKeyMap_.containsKey(key)) {
                return new SuccessfulTransportFuture(this);
            }

            try {
                MembershipKey membershipKey = channel_.join(key.group_, key.networkInterface_, source);
                membershipKeyMap_.put(key, membershipKey);
                return new SuccessfulTransportFuture(this);
            } catch (Exception e) {
                return new FailedTransportFuture(this, e);
            }
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
    public TransportFuture leave(InetAddress group, NetworkInterface networkInterface) {
        Platform.javaVersion().throwIfUnsupported(JavaVersion.JAVA7);

        final GroupKey key = new GroupKey(group, networkInterface);
        MembershipKey membershipKey = membershipKeyMap_.remove(key);
        if (membershipKey == null) {
            return new SuccessfulTransportFuture(this);
        }

        membershipKey.drop();
        return new SuccessfulTransportFuture(this);
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
        Platform.javaVersion().throwIfUnsupported(JavaVersion.JAVA7);

        GroupKey key = new GroupKey(group, networkInterface);
        final MembershipKey membershipKey = membershipKeyMap_.get(key);
        if (membershipKey == null) {
            return new SuccessfulTransportFuture(this);
        }

        try {
            membershipKey.block(source);
            return new SuccessfulTransportFuture(this);
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
        Platform.javaVersion().throwIfUnsupported(JavaVersion.JAVA7);

        GroupKey key = new GroupKey(group, networkInterface);
        final MembershipKey membershipKey = membershipKeyMap_.get(key);
        if (membershipKey == null) {
            return new SuccessfulTransportFuture(this);
        }

        try {
            membershipKey.unblock(source);
            return new SuccessfulTransportFuture(this);
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

        @Override
        public int hashCode() {
            return Arrays.hashCode(new Object[]{group_, networkInterface_});
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
