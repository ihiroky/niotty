package net.ihiroky.niotty.nio.local;

import com.sun.jna.ptr.IntByReference;
import net.ihiroky.niotty.util.Arguments;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.ScatteringByteChannel;
import java.nio.channels.SelectionKey;

/**
 *
 */
public class UnixDomainChannel extends AbstractUnixDomainChannel
        implements ScatteringByteChannel, GatheringByteChannel {

    protected UnixDomainChannel(int fd) throws IOException {
        super(fd, SelectionKey.OP_READ | SelectionKey.OP_WRITE | SelectionKey.OP_CONNECT);
    }

    private UnixDomainChannel(int fd, int validOps) throws IOException {
        super(fd, validOps);
    }

    public static UnixDomainChannel open() throws IOException {
        int fd = open(Native.SOCK_STREAM);
        return new UnixDomainChannel(fd);
    }

    public static final UnixDomainChannel[] pair() throws IOException {
        int[] sockets = { -1, -1 };
        Native.socketpair(Native.AF_UNIX, Native.SOCK_STREAM, Native.PROTOCOL, sockets);
        return new UnixDomainChannel[] {
                new UnixDomainChannel(sockets[0], SelectionKey.OP_READ | SelectionKey.OP_WRITE),
                new UnixDomainChannel(sockets[1], SelectionKey.OP_READ | SelectionKey.OP_WRITE)
        };
    }

    public synchronized UnixDomainChannel connect(SocketAddress remote) throws IOException {
        Arguments.requireNonNull(remote, "remote");
        if (!(remote instanceof UnixDomainSocketAddress)) {
            throw new IllegalArgumentException("The remote must be instance of " + UnixDomainSocketAddress.class.getName());
        }

        UnixDomainSocketAddress uds = (UnixDomainSocketAddress) remote;
        Native.SockAddrUn sa = uds.addr();
        if (Native.connect(fd_, sa, sa.size()) == -1) {
            throw new IOException(Native.getLastError());
        }
        return this;
    }

    public SocketAddress getRemoteAddress() throws IOException {
        Native.SockAddrUn sa = new Native.SockAddrUn();
        IntByReference saLen = new IntByReference();
        if (Native.getpeername(fd_, sa, saLen) == -1) {
            throw new IOException(Native.getLastError());
        }
        return new UnixDomainSocketAddress(sa);
    }

    @Override
    public UnixDomainChannel bind(SocketAddress local) throws IOException {
        UnixDomainSocketAddress uds = (UnixDomainSocketAddress) local;
        Native.SockAddrUn sa = uds.addr();
        if (Native.bind(fd_, sa, sa.size()) == -1) {
            throw new IOException(Native.getLastError());
        }
        return this;
    }

    @Override
    public long write(ByteBuffer[] srcs, int offset, int length) throws IOException {
        if (length > Native.IOV_MAX) {
            throw new IOException("The length must be less than " + Native.IOV_MAX);
        }

        Native.IOVec head = new Native.IOVec();
        Native.IOVec[] array = (Native.IOVec[]) head.toArray(length); // TODO reuse IOVec
        for (int i = 0; i < length; i++) {
            ByteBuffer src = srcs[i + offset];
            Native.IOVec ioVec = new Native.IOVec(array[i].getPointer());
            ioVec.iovBase_ = src;
            ioVec.iovLen_ = src.remaining();
        }
        long written = Native.writev(fd_, head.getPointer(), length);

        long left = written;
        for (int i = 0; i < length && left > 0; i++) {
            ByteBuffer src = srcs[i];
            int position = src.position();
            int remaining = src.remaining();
            int n = (left > remaining) ? remaining : (int) left;
            src.position(position + n);
            left -= n;
        }
        return written;
    }

    @Override
    public long write(ByteBuffer[] srcs) throws IOException {
        return write(srcs, 0, srcs.length);
    }

    @Override
    public long read(ByteBuffer[] dsts, int offset, int length) throws IOException {
        if (length > Native.IOV_MAX) {
            throw new IOException("The length must be less than " + Native.IOV_MAX);
        }

        Native.IOVec head = new Native.IOVec();
        Native.IOVec[] array = (Native.IOVec[]) head.toArray(length); // TODO reuse IOVec
        for (int i = 0; i < length; i++) {
            ByteBuffer dst = dsts[i + offset];
            Native.IOVec ioVec = new Native.IOVec(array[i].getPointer());
            ioVec.iovBase_ = dst;
            ioVec.iovLen_ = dst.remaining();
        }

        long read = Native.readv(fd_, head.getPointer(), length);

        long left = read;
        for (int i = 0; i < length && left > 0; i++) {
            ByteBuffer dst = dsts[i];
            int position = dst.position();
            int remaining = dst.remaining();
            int n = (left > remaining) ? remaining : (int) left;
            dst.position(position + n);
            left -= n;
        }
        return read;
    }

    @Override
    public long read(ByteBuffer[] dsts) throws IOException {
        return read(dsts, 0, dsts.length);
    }

    @Override
    public int read(ByteBuffer dst) throws IOException {
        int read = Native.read(fd_, dst, dst.remaining());
        if (read > 0) {
            dst.position(dst.position() + read);
        }
        return read;
    }

    @Override
    public int write(ByteBuffer src) throws IOException {
        int written = Native.write(fd_, src, src.remaining());
        if (written > 0) {
            src.position(src.position() + written);
        }
        return written;
    }

    public int send(ByteBuffer src, SocketAddress target) throws IOException {
        UnixDomainSocketAddress sa = (UnixDomainSocketAddress) target;
        Native.SockAddrUn sun = sa.addr();
        int sent = Native.sendto(fd_, src, src.remaining(), 0, sun, sun.size());
        if (sent == -1) {
            throw new IOException(Native.getLastError());
        }
        src.position(src.position() + sent);
        return sent;
    }

    public SocketAddress receive(ByteBuffer src) throws IOException {
        Native.SockAddrUn source = new Native.SockAddrUn();
        IntByReference size = new IntByReference();
        int received = Native.recvfrom(fd_, src, src.remaining(), 0, source, size);
        if (received == -1) {
            throw new IOException(Native.getLastError());
        }
        return received > 0 ? new UnixDomainSocketAddress(source) : null;
    }

    public UnixDomainChannel shutdownInput() throws IOException {
        if (Native.shutdown(fd_, Native.SHUT_RD) == -1) {
            throw new IOException(Native.getLastError());
        }
        return this;
    }

    public UnixDomainChannel shutdownOutput() throws IOException {
        if (Native.shutdown(fd_, Native.SHUT_WR) == -1) {
            throw new IOException(Native.getLastError());
        }
        return this;
    }
}
