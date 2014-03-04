package net.ihiroky.niotty.nio.unix;

import net.ihiroky.niotty.util.Arguments;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.AlreadyBoundException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ConnectionPendingException;
import java.nio.channels.NoConnectionPendingException;
import java.nio.channels.NotYetConnectedException;
import java.nio.channels.SelectionKey;

/**
 *
 */
public class ClientUnixDomainChannel extends ReadWriteUnixDomainChannel {

    private volatile ConnectionState connectionState_;
    private int shutdownState_;
    private UnixDomainSocketAddress remote_;

    private int SHUTDOWN_INPUT = 1;
    private int SHUTDOWN_OUTPUT = 1 << 1;

    enum ConnectionState {
        INITIAL,
        CONNECTING,
        CONNECTED,
    }

    protected ClientUnixDomainChannel(int fd, boolean connected) throws IOException {
        super(fd);
        connectionState_ = connected ? ConnectionState.CONNECTED : ConnectionState.INITIAL;
    }

    private ClientUnixDomainChannel(int fd, int validOps) throws IOException {
        super(fd, validOps);
        connectionState_ = ConnectionState.INITIAL;
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
            if ((shutdownState_ & SHUTDOWN_INPUT) != 0) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected boolean ensureWriteOpen() throws IOException {
        synchronized (stateLock_) {
            if (!isOpen()) {
                throw new ClosedChannelException();
            }
            if (!isConnected()) {
                throw new NotYetConnectedException();
            }
            if ((shutdownState_ & SHUTDOWN_OUTPUT) != 0) {
                return false;
            }
        }
        return true;
    }

    public static ClientUnixDomainChannel open() throws IOException {
        int fd = open(Native.SOCK_STREAM);
        return new ClientUnixDomainChannel(fd, false);
    }

    public static final ClientUnixDomainChannel[] pair() throws IOException {
        int[] sockets = { -1, -1 };
        Native.socketpair(Native.AF_UNIX, Native.SOCK_STREAM, Native.PROTOCOL, sockets);
        int ops = SelectionKey.OP_READ | SelectionKey.OP_WRITE;
        return new ClientUnixDomainChannel[] {
                new ClientUnixDomainChannel(sockets[0], ops),
                new ClientUnixDomainChannel(sockets[1], ops)
        };
    }

    private boolean connect() throws IOException {
        synchronized (stateLock_) {
            if (!isOpen()) {
                return false;
            }
        }

        synchronized (blockingLock()) {
            if (connectionState_ == ConnectionState.CONNECTED) {
                return true;
            }
            connectionState_ = ConnectionState.CONNECTING;
            Native.SockAddrUn sun = AddressBuffer.getInstance().getAddress();
            sun.setSunPath(remote_.getPath());
            try {
                begin();
                if (Native.connect(fd_, sun, sun.size()) == -1) {
                    switch (Native.errno()) {
                        case Native.EISCONN:
                        connectionState_ = ConnectionState.CONNECTED;
                            return true;
                        case Native.EALREADY:
                            return false;
                        default:
                            throw new IOException(Native.getLastError());
                    }
                }
                connectionState_ = ConnectionState.CONNECTED;
            } finally {
                end(connectionState_ == ConnectionState.CONNECTED);
            }
        }
        return true;
    }

    public boolean connect(SocketAddress remote) throws IOException {
        Arguments.requireNonNull(remote, "remote");
        remote_ = (UnixDomainSocketAddress) remote;
        return connect();
    }

    public boolean finishConnect() throws IOException {
        switch (connectionState_) {
            case INITIAL:
                throw new NoConnectionPendingException();
            case CONNECTING:
                return connect();
            case CONNECTED:
                return true;
            default:
                throw new AssertionError("Invalid state: " + connectionState_);
        }
    }

    public boolean isConnected() throws IOException {
        return connectionState_ == ConnectionState.CONNECTED;
    }

    public boolean isConnectionPending() throws IOException {
        return connectionState_ == ConnectionState.CONNECTING;
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
    public ClientUnixDomainChannel bind(SocketAddress local) throws IOException {
        UnixDomainSocketAddress uds = (UnixDomainSocketAddress) local;
        synchronized (stateLock_) {
            if (!isOpen()) {
                throw new ClosedChannelException();
            }
            if (connectionState_ == ConnectionState.CONNECTING) {
                throw new ConnectionPendingException();
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

    public ClientUnixDomainChannel shutdownInput() throws IOException {
        synchronized (stateLock_) {
            if (!isOpen()) {
                throw new ClosedChannelException();
            }
            if (!isConnected()) {
                throw new NotYetConnectedException();
            }
            if ((shutdownState_ & SHUTDOWN_INPUT) == 0) {
                if (Native.shutdown(fd_, Native.SHUT_RD) == -1) {
                    throw new IOException(Native.getLastError());
                }
                shutdownState_ |= SHUTDOWN_INPUT;
            }
        }
        return this;
    }

    public ClientUnixDomainChannel shutdownOutput() throws IOException {
        synchronized (stateLock_) {
            if (!isOpen()) {
                throw new ClosedChannelException();
            }
            if (!isConnected()) {
                throw new NotYetConnectedException();
            }
            if ((shutdownState_ & SHUTDOWN_OUTPUT) == 0) {
                if (Native.shutdown(fd_, Native.SHUT_WR) == -1) {
                    throw new IOException(Native.getLastError());
                }
                shutdownState_ |= SHUTDOWN_OUTPUT;
            }
        }
        return this;
    }
}
