package net.ihiroky.niotty.nio.unix;

import com.sun.jna.ptr.IntByReference;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.NetworkChannel;
import java.nio.channels.SelectionKey;

/**
 *
 */
public class ServerUnixDomainChannel extends AbstractUnixDomainChannel implements NetworkChannel {

    private static final int DEFAULT_BACKLOG = 64;

    protected ServerUnixDomainChannel(int fd) throws IOException {
        super(fd, SelectionKey.OP_ACCEPT);
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
        Native.SockAddrUn sa = uds.addr();
        if (Native.bind(fd_, sa, sa.size()) == -1) {
            throw new IOException(Native.getLastError());
        }
        if (Native.listen(fd_, backlog) == -1) {
            throw new IOException(Native.getLastError());
        }
        return this;
    }

    public synchronized UnixDomainChannel accept() throws IOException {
        Native.SockAddrUn caddr = new Native.SockAddrUn();
        IntByReference caddrLength = new IntByReference();
        int client = Native.accept(fd_, caddr, caddrLength);
        if (client == -1) {
            throw new IOException(Native.getLastError());
        }

        return new UnixDomainChannel(client);
    }
}
