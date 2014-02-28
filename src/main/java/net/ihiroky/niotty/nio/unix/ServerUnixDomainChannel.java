package net.ihiroky.niotty.nio.unix;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.NetworkChannel;
import java.nio.channels.SelectionKey;

/**
 *
 */
public class ServerUnixDomainChannel extends AbstractUnixDomainChannel implements NetworkChannel {

    private final Object acceptLock_;

    private static final int DEFAULT_BACKLOG = 64;

    protected ServerUnixDomainChannel(int fd) throws IOException {
        super(fd, SelectionKey.OP_ACCEPT);
        acceptLock_ = new Object();
    }

    public static ServerUnixDomainChannel open() throws IOException {
        int fd = open(Native.SOCK_STREAM);
        return new ServerUnixDomainChannel(fd);
    }

    @Override
    public ServerUnixDomainChannel bind(SocketAddress local) throws IOException {
        return bind(local, DEFAULT_BACKLOG);
    }

    public synchronized ServerUnixDomainChannel bind(SocketAddress local, int backlog) throws IOException {
        UnixDomainSocketAddress uds = (UnixDomainSocketAddress) local;
        synchronized (stateLock_) {
            Native.SockAddrUn sun = AddressBuffer.getInstance().getAddress();
            sun.clear();
            sun.setSunPath(uds.getPath());
            if (Native.bind(fd_, sun, sun.size()) == -1) {
                throw new IOException(Native.getLastError());
            }
            if (Native.listen(fd_, backlog) == -1) {
                throw new IOException(Native.getLastError());
            }
        }
        return this;
    }

    public synchronized UnixDomainChannel accept() throws IOException {
        int client = -1;
        synchronized (acceptLock_) {
            AddressBuffer buffer = AddressBuffer.getInstance();
            Native.SockAddrUn sun = buffer.getAddress();
            sun.clear();
            try {
                begin();
                client = Native.accept(fd_, sun, buffer.getSize());
            } finally {
                end(client != -1);
            }
            if (client == -1) {
                throw new IOException(Native.getLastError());
            }
        }

        UnixDomainChannel channel = new UnixDomainChannel(client);

        // call get*Address() to cache the addresses.
        channel.getLocalAddress();
        channel.getRemoteAddress();
        return new UnixDomainChannel(client);
    }
}
