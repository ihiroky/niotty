package net.ihiroky.niotty.nio.unix;

import net.ihiroky.niotty.util.Arguments;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AlreadyBoundException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.NotYetConnectedException;
import java.nio.channels.SelectionKey;
import java.util.Arrays;

/**
 * 3/4 TODO refine exception throwing
 */
public class DatagramUnixDomainChannel extends ReadWriteUnixDomainChannel {

    private UnixDomainSocketAddress remote_;

    private DatagramUnixDomainChannel(int fd) throws IOException {
        this(fd, SelectionKey.OP_READ | SelectionKey.OP_WRITE | SelectionKey.OP_CONNECT);
    }

    private DatagramUnixDomainChannel(int fd, int validOps) throws IOException {
        super(fd, validOps);
    }

    @Override
    protected boolean ensureReadOpen() throws IOException {
        synchronized (stateLock_) {
            if (!isOpen()) {
                throw new ClosedChannelException();
            }
            if (!isConnected()) {
                throw new NotYetConnectedException();
            }
        }
        return false;
    }

    @Override
    protected boolean ensureWriteOpen() throws IOException {
        return false;
    }

    public static DatagramUnixDomainChannel open() throws IOException {
        int fd = open(Native.SOCK_DGRAM);
        return new DatagramUnixDomainChannel(fd);
    }

    public static final DatagramUnixDomainChannel[] pair() throws IOException {
        int[] sockets = { -1, -1 };
        Native.socketpair(Native.AF_UNIX, Native.SOCK_DGRAM, Native.PROTOCOL, sockets);
        int ops = SelectionKey.OP_READ | SelectionKey.OP_WRITE;
        return new DatagramUnixDomainChannel[] {
                new DatagramUnixDomainChannel(sockets[0], ops),
                new DatagramUnixDomainChannel(sockets[1], ops)
        };
    }

    public DatagramUnixDomainChannel connect(SocketAddress remote) throws IOException {
        Arguments.requireNonNull(remote, "remote");
        synchronized (stateLock_) {
            if (!isOpen()) {
                throw new ClosedChannelException();
            }
            if (remote_ != null) {
                throw new IllegalStateException("The connect() is already invoked.");
            }

            Native.SockAddrUn sun = AddressBuffer.getInstance().getAddress();
            sun.setSunPath(remote_.getPath());
            if (Native.connect(fd_, sun, sun.size()) == -1) {
                throw new IOException(Native.getLastError());
            }
            remote_ = (UnixDomainSocketAddress) remote;
        }
        return this;
    }

    public DatagramUnixDomainChannel disconnect() throws IOException{
        synchronized (stateLock_) {
            if (!isOpen()) {
                throw new ClosedChannelException();
            }
            if (remote_ == null) {
                return this;
            }

            // Ref: http://timesinker.blogspot.jp/2010/02/unconnect-udp-socket.html
            Native.SockAddrUn sun = AddressBuffer.getInstance().getAddress();
            sun.clear();
            sun.sun_family_ = Native.AF_UNSPEC;
            Arrays.fill(sun.sun_path_, (byte) 0);
            if (Native.connect(fd_, sun, sun.size()) == -1) {
                throw new IOException(Native.getLastError());
            }
            remote_ = null;
        }
        return this;
    }

    public boolean isConnected() throws IOException {
        synchronized (stateLock_) {
            return remote_ != null;
        }
    }

    public SocketAddress getRemoteAddress() throws IOException {
        synchronized (stateLock_) {
            if (remote_ == null) {
                AddressBuffer buffer = AddressBuffer.getInstance();
                Native.SockAddrUn sun = buffer.getAddress();
                if (Native.getpeername(fd_, sun, buffer.getSize()) == -1) {
                    throw new IOException(Native.getLastError());
                }
                remote_ = new UnixDomainSocketAddress(sun.getSunPath());
            }
        }
        return remote_;
    }

    @Override
    public DatagramUnixDomainChannel bind(SocketAddress local) throws IOException {
        UnixDomainSocketAddress uds = (UnixDomainSocketAddress) local;
        synchronized (stateLock_) {
            if (!isOpen()) {
                throw new ClosedChannelException();
            }
            if (localAddress_ != null) {
                throw new AlreadyBoundException();
            }

            Native.SockAddrUn sun = AddressBuffer.getInstance().getAddress();
            sun.clear();
            sun.setSunPath(uds.getPath());
            if (Native.bind(fd_, sun, sun.size()) == -1) {
                throw new IOException(Native.getLastError());
            }
            localAddress_ = uds;
        }
        return this;
    }

    public int send(ByteBuffer src, SocketAddress target) throws IOException {
        synchronized (stateLock_) {
            if (!isOpen()) {
                throw new ClosedChannelException();
            }
            if (isConnected() && !remote_.equals(target)) {
                throw new IOException("The target is not equals to the connected address.");
            }
        }

        UnixDomainSocketAddress sa = (UnixDomainSocketAddress) target;
        int sent = 0;
        Native.SockAddrUn sun = AddressBuffer.getInstance().getAddress();
        sun.setSunPath(sa.getPath());
        try {
            begin();
            sent = Native.sendto(fd_, src, src.remaining(), 0, sun, sun.size());
        } finally {
            end(sent > 0);
        }
        if (sent == -1) {
            throw new IOException(Native.getLastError());
        }
        src.position(src.position() + sent);
        return sent;
    }

    public SocketAddress receive(ByteBuffer src) throws IOException {
        synchronized (stateLock_) {
            if (!isOpen()) {
                return null;
            }
        }

        int received = 0;
        String sunPath;
        AddressBuffer addressBuffer = AddressBuffer.getInstance();
        Native.SockAddrUn sun = addressBuffer.getAddress();
        try {
            begin();
            received = Native.recvfrom(fd_, src, src.remaining(), 0, sun, addressBuffer.getSize());
        } finally {
            end(received > 0);
        }
        if (received == -1) {
            if (Native.errno() == Native.EAGAIN) {
                return null;
            }
            throw new IOException(Native.getLastError());
        }
        sunPath = sun.getSunPath();
        src.position(src.position() + received);
        return received >= 0 ? new UnixDomainSocketAddress(sunPath) : null;
    }
}
