package net.ihiroky.niotty.nio.unix;

import com.sun.jna.ptr.IntByReference;

import java.io.IOException;
import java.net.SocketAddress;
import java.net.SocketOption;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.NetworkChannel;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 *
 *
 * An instance of this class accept {@link net.ihiroky.niotty.nio.unix.UnixDomainSocketAddress} only.
 * Implementations throw ClassCastException on attempts to use any operations.
 */
public abstract class AbstractUnixDomainChannel extends AbstractSelectableChannel implements NetworkChannel {

    protected final int fd_;
    protected final Object lock_;
    private final int validOps_;
    private int ops_;

    private static final Set<SocketOption<?>> SUPPORTED_OPTIONS =
            Collections.unmodifiableSet(new HashSet<SocketOption<?>>(Arrays.asList(
                    StandardSocketOptions.SO_RCVBUF, StandardSocketOptions.SO_SNDBUF, SocketOptions.SO_PASSCRED)));

    protected AbstractUnixDomainChannel(int fd, int validOps) throws IOException {
        super(null);

        fd_ = fd;
        validOps_ = validOps;
        lock_ = new Object();
    }

    protected static int open(int type) throws IOException {
        int fd = Native.socket(Native.AF_UNIX, type, Native.PROTOCOL);
        if (fd == -1) {
            throw new IOException(Native.getLastError());
        }
        return fd;
    }

    @Override
    protected void implCloseSelectableChannel() throws IOException {
        synchronized (lock_) {
            if (Native.shutdown(fd_, Native.SHUT_RDWR) == -1) {
                throw new IOException(Native.getLastError());
            }
            if (Native.close(fd_) == -1) {
                throw new IOException(Native.getLastError());
            }
        }
    }

    @Override
    protected synchronized void implConfigureBlocking(boolean block) throws IOException {
        int flags = Native.fcntl(fd_, Native.F_GETFL, 0);
        if (block) {
            flags |= Native.O_NONBLOCK;
        } else {
            flags &= ~Native.O_NONBLOCK;
        }
        Native.fcntl(fd_, Native.F_SETFL, flags);
    }

    @Override
    public SocketAddress getLocalAddress() throws IOException {
        Native.SockAddrUn sa = new Native.SockAddrUn();
        IntByReference saLen = new IntByReference();
        if (Native.getsockname(fd_, sa, saLen) == -1) {
            throw new IOException(Native.getLastError());
        }
        return new UnixDomainSocketAddress(sa);
    }


    @Override
    public <T> NetworkChannel setOption(SocketOption<T> name, T value) throws IOException {
        if (name.equals(StandardSocketOptions.SO_RCVBUF)) {
            ByteBuffer buffer = ByteBuffer.allocate(4).order(ByteOrder.nativeOrder());
            if (Native.setsockopt(fd_, Native.SOL_SOCKET, Native.SO_RCVBUF, buffer, buffer.capacity()) == -1) {
                throw new IOException(Native.getLastError());
            }
            return this;
        } else if (name.equals(StandardSocketOptions.SO_SNDBUF))                    {
            ByteBuffer buffer = ByteBuffer.allocate(4).order(ByteOrder.nativeOrder());
            if (Native.setsockopt(fd_, Native.SOL_SOCKET, Native.SO_SNDBUF, buffer, buffer.capacity()) == -1) {
                throw new IOException(Native.getLastError());
            }
            return this;
        } else if (name.equals(SocketOptions.SO_PASSCRED)) {
            ByteBuffer buffer = ByteBuffer.allocate(4).order(ByteOrder.nativeOrder());
            if (Native.setsockopt(fd_, Native.SOL_SOCKET, Native.SO_PASSCRED, buffer, buffer.capacity()) == -1) {
                throw new IOException(Native.getLastError());
            }
            return this;
        }
        throw new UnsupportedOperationException(name.name());
    }

    @Override
    public <T> T getOption(SocketOption<T> name) throws IOException {
        if (name.equals(StandardSocketOptions.SO_RCVBUF)) {
            ByteBuffer buffer = ByteBuffer.allocate(4).order(ByteOrder.nativeOrder());
            IntByReference bufLen = new IntByReference();
            if (Native.getsockopt(fd_, Native.SOL_SOCKET, Native.SO_RCVBUF, buffer, bufLen) == -1) {
                throw new IOException(Native.getLastError());
            }
            return name.type().cast(buffer.getInt());
        } else if (name.equals(StandardSocketOptions.SO_SNDBUF))                    {
            ByteBuffer buffer = ByteBuffer.allocate(4).order(ByteOrder.nativeOrder());
            IntByReference bufLen = new IntByReference();
            if (Native.getsockopt(fd_, Native.SOL_SOCKET, Native.SO_SNDBUF, buffer, bufLen) == -1) {
                throw new IOException(Native.getLastError());
            }
            return name.type().cast(buffer.getInt());
        } else if (name.equals(SocketOptions.SO_PASSCRED)) {
            ByteBuffer buffer = ByteBuffer.allocate(4).order(ByteOrder.nativeOrder());
            IntByReference bufLen = new IntByReference();
            if (Native.getsockopt(fd_, Native.SOL_SOCKET, Native.SO_PASSCRED, buffer, bufLen) == -1) {
                throw new IOException(Native.getLastError());
            }
            return name.type().cast(buffer.getInt() != 0);
        }
        throw new UnsupportedOperationException(name.name());
    }

    @Override
    public Set<SocketOption<?>> supportedOptions() {
        return SUPPORTED_OPTIONS;
    }

    @Override
    public int validOps() {
        return validOps_;
    }
}
