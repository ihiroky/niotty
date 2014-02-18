package net.ihiroky.niotty.nio.unix;

import com.sun.jna.ptr.IntByReference;

import java.io.IOException;
import java.net.SocketAddress;
import java.net.SocketOption;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.NetworkChannel;
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
public abstract class AbstractUnixDomainChannel extends AbstractChannel {

    protected final Native.SockAddrUn ADDRESS_BUFFER = new Native.SockAddrUn();
    protected final IntByReference ADDRESS_SIZE_BUFFER = new IntByReference();

    private static final Set<SocketOption<?>> SUPPORTED_OPTIONS =
            Collections.unmodifiableSet(new HashSet<SocketOption<?>>(Arrays.asList(
                    StandardSocketOptions.SO_RCVBUF, StandardSocketOptions.SO_SNDBUF, SocketOptions.SO_PASSCRED)));

    protected AbstractUnixDomainChannel(int fd, int validOps) throws IOException {
        super(fd, validOps);
    }

    protected static int open(int type) throws IOException {
        int fd = Native.socket(Native.AF_UNIX, type, Native.PROTOCOL);
        if (fd == -1) {
            throw new IOException(Native.getLastError());
        }
        return fd;
    }

    @Override
    public SocketAddress getLocalAddress() throws IOException {
        Native.SockAddrUn sa = ADDRESS_BUFFER;
        IntByReference saLen = ADDRESS_SIZE_BUFFER;
        String sunPath;
        synchronized (lock_) {
            sa.clear();
            if (Native.getsockname(fd_, sa, saLen) == -1) {
                throw new IOException(Native.getLastError());
            }
            sunPath = sa.getSunPath();
        }
        return new UnixDomainSocketAddress(sunPath);
    }


    @Override
    public <T> NetworkChannel setOption(SocketOption<T> name, T value) throws IOException {
        if (name.equals(StandardSocketOptions.SO_RCVBUF)) {
            ByteBuffer buffer = ByteBuffer.allocate(4).order(ByteOrder.nativeOrder());
            synchronized (lock_) {
                if (Native.setsockopt(fd_, Native.SOL_SOCKET, Native.SO_RCVBUF, buffer, buffer.capacity()) == -1) {
                    throw new IOException(Native.getLastError());
                }
            }
            return this;
        } else if (name.equals(StandardSocketOptions.SO_SNDBUF))                    {
            ByteBuffer buffer = ByteBuffer.allocate(4).order(ByteOrder.nativeOrder());
            synchronized (lock_) {
                if (Native.setsockopt(fd_, Native.SOL_SOCKET, Native.SO_SNDBUF, buffer, buffer.capacity()) == -1) {
                    throw new IOException(Native.getLastError());
                }
            }
            return this;
        } else if (name.equals(SocketOptions.SO_PASSCRED)) {
            ByteBuffer buffer = ByteBuffer.allocate(4).order(ByteOrder.nativeOrder());
            synchronized (lock_) {
                if (Native.setsockopt(fd_, Native.SOL_SOCKET, Native.SO_PASSCRED, buffer, buffer.capacity()) == -1) {
                    throw new IOException(Native.getLastError());
                }
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
            synchronized (lock_) {
                if (Native.getsockopt(fd_, Native.SOL_SOCKET, Native.SO_RCVBUF, buffer, bufLen) == -1) {
                    throw new IOException(Native.getLastError());
                }
            }
            return name.type().cast(buffer.getInt());
        } else if (name.equals(StandardSocketOptions.SO_SNDBUF))                    {
            ByteBuffer buffer = ByteBuffer.allocate(4).order(ByteOrder.nativeOrder());
            IntByReference bufLen = new IntByReference();
            synchronized (lock_) {
                if (Native.getsockopt(fd_, Native.SOL_SOCKET, Native.SO_SNDBUF, buffer, bufLen) == -1) {
                    throw new IOException(Native.getLastError());
                }
            }
            return name.type().cast(buffer.getInt());
        } else if (name.equals(SocketOptions.SO_PASSCRED)) {
            ByteBuffer buffer = ByteBuffer.allocate(4).order(ByteOrder.nativeOrder());
            IntByReference bufLen = new IntByReference();
            synchronized (lock_) {
                if (Native.getsockopt(fd_, Native.SOL_SOCKET, Native.SO_PASSCRED, buffer, bufLen) == -1) {
                    throw new IOException(Native.getLastError());
                }
            }
            return name.type().cast(buffer.getInt() != 0);
        }
        throw new UnsupportedOperationException(name.name());
    }

    @Override
    public Set<SocketOption<?>> supportedOptions() {
        return SUPPORTED_OPTIONS;
    }
}