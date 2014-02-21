package net.ihiroky.niotty.nio.unix;

import net.ihiroky.niotty.util.Arguments;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.NoConnectionPendingException;
import java.nio.channels.ScatteringByteChannel;
import java.nio.channels.SelectionKey;

/**
 *
 */
public class UnixDomainChannel extends AbstractUnixDomainChannel
        implements ScatteringByteChannel, GatheringByteChannel {

    private volatile State state_;
    private UnixDomainSocketAddress remote_;
    private final Object ioLock_;

    private final Native.IOVec IOVEC_HEAD = new Native.IOVec();
    private final Native.IOVec[] IOVEC_ARRAY = (Native.IOVec[]) IOVEC_HEAD.toArray(Native.IOV_MAX);

    private enum State {
        OPEN,
        CONNECTING,
        CONNECTED,
    }

    protected UnixDomainChannel(int fd) throws IOException {
        this(fd, SelectionKey.OP_READ | SelectionKey.OP_WRITE | SelectionKey.OP_CONNECT);
    }

    private UnixDomainChannel(int fd, int validOps) throws IOException {
        super(fd, validOps);
        state_ = State.OPEN;
        ioLock_ = new Object();
    }

    public static UnixDomainChannel open() throws IOException {
        int fd = open(Native.SOCK_STREAM);
        return new UnixDomainChannel(fd);
    }

    public static final UnixDomainChannel[] pair() throws IOException {
        int[] sockets = { -1, -1 };
        Native.socketpair(Native.AF_UNIX, Native.SOCK_STREAM, Native.PROTOCOL, sockets);
        int ops = SelectionKey.OP_READ | SelectionKey.OP_WRITE;
        return new UnixDomainChannel[] {
                new UnixDomainChannel(sockets[0], ops),
                new UnixDomainChannel(sockets[1], ops)
        };
    }

    public static UnixDomainChannel openDatagram() throws IOException {
        int fd = open(Native.SOCK_DGRAM);
        return new UnixDomainChannel(fd);
    }

    public static final UnixDomainChannel[] pairDatagram() throws IOException {
        int[] sockets = { -1, -1 };
        Native.socketpair(Native.AF_UNIX, Native.SOCK_DGRAM, Native.PROTOCOL, sockets);
        int ops = SelectionKey.OP_READ | SelectionKey.OP_WRITE;
        return new UnixDomainChannel[] {
                new UnixDomainChannel(sockets[0], ops),
                new UnixDomainChannel(sockets[1], ops)
        };
    }

    private boolean connect() throws IOException {
        if (state_ == State.CONNECTED) {
            return true;
        }

        Native.SockAddrUn address = ADDRESS_BUFFER;
        synchronized (stateLock_) {
            state_ = State.CONNECTING;
            address.clear();
            address.setSunPath(remote_.getPath());
            if (Native.connect(fd_, address, address.size()) == -1) {
                switch (Native.errno()) {
                    case Native.EISCONN:
                        state_ = State.CONNECTED;
                        return true;
                    case Native.EAGAIN:
                        return false;
                    default:
                        throw new IOException(Native.getLastError());
                }
            }
            state_ = State.CONNECTED;
        }
        return true;
    }

    public boolean connect(SocketAddress remote) throws IOException {
        Arguments.requireNonNull(remote, "remote");
        remote_ = (UnixDomainSocketAddress) remote;
        return connect();
    }

    public boolean finishConnect() throws IOException {
        switch (state_) {
            case OPEN:
                throw new NoConnectionPendingException();
            case CONNECTING:
                return connect();
            case CONNECTED:
                return true;
            default:
                throw new AssertionError("Invalid state: " + state_);
        }
    }

    public boolean isConnected() throws IOException {
        return state_ == State.CONNECTED;
    }

    public boolean isConnectionPending() throws IOException {
        return state_ == State.CONNECTING;
    }

    public SocketAddress getRemoteAddress() throws IOException {
        if (remote_ != null) {
            return remote_;
        }

        Native.SockAddrUn address = ADDRESS_BUFFER;
        String sunPath;
        synchronized (stateLock_) {
            address.clear();
            if (Native.getpeername(fd_, address, SIZE_BUFFER) == -1) {
                throw new IOException(Native.getLastError());
            }
            sunPath = address.getSunPath();
        }
        remote_ = new UnixDomainSocketAddress(sunPath);
        return remote_;
    }

    @Override
    public UnixDomainChannel bind(SocketAddress local) throws IOException {
        UnixDomainSocketAddress uds = (UnixDomainSocketAddress) local;
        Native.SockAddrUn address = ADDRESS_BUFFER;
        synchronized (stateLock_) {
            address.clear();
            address.setSunPath(uds.getPath());
            if (Native.bind(fd_, address, address.size()) == -1) {
                throw new IOException(Native.getLastError());
            }
        }
        return this;
    }

    @Override
    public long write(ByteBuffer[] srcs, int offset, int length) throws IOException {
        if (length > Native.IOV_MAX) {
            throw new IOException("The length must be less than " + Native.IOV_MAX);
        }

        long written = 0;
        synchronized (ioLock_) {
            Native.IOVec head = IOVEC_HEAD;
            Native.IOVec[] array = IOVEC_ARRAY;
            for (int i = 0; i < length; i++) {
                ByteBuffer src = srcs[i + offset];
                Native.IOVec ioVec = new Native.IOVec(array[i].getPointer());
                ioVec.iovBase_ = src;
                ioVec.iovLen_ = src.remaining();
            }
            try {
                begin();
                written = Native.writev(fd_, head.getPointer(), length);
            } finally {
                end(written > 0);
            }
            if (written == -1) {
                throw new IOException(Native.getLastError());
            }
        }

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

        long read = 0;
        synchronized (ioLock_) {
            Native.IOVec head = IOVEC_HEAD;
            Native.IOVec[] array = IOVEC_ARRAY;
            for (int i = 0; i < length; i++) {
                ByteBuffer dst = dsts[i + offset];
                Native.IOVec ioVec = new Native.IOVec(array[i].getPointer());
                ioVec.iovBase_ = dst;
                ioVec.iovLen_ = dst.remaining();
            }
            try {
                begin();
                read = Native.readv(fd_, head.getPointer(), length);
            } finally {
                end(read > 0);
            }
            if (read == -1) {
                throw new IOException(Native.getLastError());
            }
        }

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
        int read = 0;
        synchronized (ioLock_) {
            try {
                begin();
                read = Native.read(fd_, dst, dst.remaining());
            } finally {
                end(read > 0);
            }
            if (read == -1) {
                if (Native.errno() == Native.EAGAIN) {
                    return 0;
                }
                throw new IOException(Native.getLastError());
            }
        }
        if (read == 0) { // EOF according to 'man 2 read'
            return -1;
        }
        dst.position(dst.position() + read);
        return read;
    }

    @Override
    public int write(ByteBuffer src) throws IOException {
        int written;
        synchronized (stateLock_) {
            written = Native.write(fd_, src, src.remaining());
            if (written == -1) {
                throw new IOException(Native.getLastError());
            }
        }
        src.position(src.position() + written);
        return written;
    }

    public int send(ByteBuffer src, SocketAddress target) throws IOException {
        UnixDomainSocketAddress sa = (UnixDomainSocketAddress) target;
        int sent = 0;
        Native.SockAddrUn address = ADDRESS_BUFFER;
        synchronized (stateLock_) {
            address.clear();
            address.setSunPath(sa.getPath());
            try {
                begin();
                sent = Native.sendto(fd_, src, src.remaining(), 0, address, address.size());
            } finally {
                end(sent > 0);
            }
            if (sent == -1) {
                throw new IOException(Native.getLastError());
            }
        }
        src.position(src.position() + sent);
        return sent;
    }

    public SocketAddress receive(ByteBuffer src) throws IOException {
        Native.SockAddrUn source = ADDRESS_BUFFER;
        int received = 0;
        String sunPath;
        synchronized (stateLock_) {
            source.clear();
            try {
                begin();
                received = Native.recvfrom(fd_, src, src.remaining(), 0, source, SIZE_BUFFER);
            } finally {
                end(received > 0);
            }
            if (received == -1) {
                throw new IOException(Native.getLastError());
            }
            sunPath = source.getSunPath();
        }
        src.position(src.position() + received);
        return received >= 0 ? new UnixDomainSocketAddress(sunPath) : null;
    }

    public UnixDomainChannel shutdownInput() throws IOException {
        synchronized (stateLock_) {
            if (Native.shutdown(fd_, Native.SHUT_RD) == -1) {
                throw new IOException(Native.getLastError());
            }
        }
        return this;
    }

    public UnixDomainChannel shutdownOutput() throws IOException {
        synchronized (stateLock_) {
            if (Native.shutdown(fd_, Native.SHUT_WR) == -1) {
                throw new IOException(Native.getLastError());
            }
        }
        return this;
    }
}
