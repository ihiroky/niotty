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

    protected final AddressBuffer addressBuffer_;

    protected static class AddressBuffer {
        final Native.SockAddrUn address_ = new Native.SockAddrUn();
        final IntByReference size_ = new IntByReference();
    }

    private static final Set<SocketOption<?>> SUPPORTED_OPTIONS =
            Collections.unmodifiableSet(new HashSet<SocketOption<?>>(Arrays.<SocketOption<?>>asList(
                    StandardSocketOptions.SO_RCVBUF, StandardSocketOptions.SO_SNDBUF, SocketOptions.SO_PASSCRED)));

    protected AbstractUnixDomainChannel(int fd, int validOps) throws IOException {
        super(fd, validOps);
        addressBuffer_ = new AddressBuffer();
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
        String sunPath;
        synchronized (addressBuffer_) {
            AddressBuffer buffer = addressBuffer_;
            Native.SockAddrUn sun = buffer.address_;
            IntByReference sunSize = buffer.size_;
            if (Native.getsockname(fd_, sun, sunSize) == -1) {
                throw new IOException(Native.getLastError());
            }
            sunPath = sun.getSunPath();
        }
        return new UnixDomainSocketAddress(sunPath);
    }


    @Override
    public <T> NetworkChannel setOption(SocketOption<T> name, T value) throws IOException {
        if (name.equals(StandardSocketOptions.SO_RCVBUF)) {
            ByteBuffer buffer = ByteBuffer.allocate(4).order(ByteOrder.nativeOrder());
            synchronized (stateLock_) {
                if (Native.setsockopt(fd_, Native.SOL_SOCKET, Native.SO_RCVBUF, buffer, buffer.capacity()) == -1) {
                    throw new IOException(Native.getLastError());
                }
            }
            return this;
        } else if (name.equals(StandardSocketOptions.SO_SNDBUF))                    {
            ByteBuffer buffer = ByteBuffer.allocate(4).order(ByteOrder.nativeOrder());
            synchronized (stateLock_) {
                if (Native.setsockopt(fd_, Native.SOL_SOCKET, Native.SO_SNDBUF, buffer, buffer.capacity()) == -1) {
                    throw new IOException(Native.getLastError());
                }
            }
            return this;
        } else if (name.equals(SocketOptions.SO_PASSCRED)) {
            ByteBuffer buffer = ByteBuffer.allocate(4).order(ByteOrder.nativeOrder());
            synchronized (stateLock_) {
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
            synchronized (addressBuffer_) {
                if (Native.getsockopt(fd_, Native.SOL_SOCKET, Native.SO_RCVBUF, buffer, addressBuffer_.size_) == -1) {
                    throw new IOException(Native.getLastError());
                }
            }
            return name.type().cast(buffer.getInt());
        } else if (name.equals(StandardSocketOptions.SO_SNDBUF))                    {
            ByteBuffer buffer = ByteBuffer.allocate(4).order(ByteOrder.nativeOrder());
            synchronized (addressBuffer_) {
                if (Native.getsockopt(fd_, Native.SOL_SOCKET, Native.SO_SNDBUF, buffer, addressBuffer_.size_) == -1) {
                    throw new IOException(Native.getLastError());
                }
            }
            return name.type().cast(buffer.getInt());
        } else if (name.equals(SocketOptions.SO_PASSCRED)) {
            ByteBuffer buffer = ByteBuffer.allocate(4).order(ByteOrder.nativeOrder());
            synchronized (addressBuffer_) {
                if (Native.getsockopt(fd_, Native.SOL_SOCKET, Native.SO_PASSCRED, buffer, addressBuffer_.size_) == -1) {
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
