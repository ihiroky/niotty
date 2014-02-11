package net.ihiroky.niotty.nio.local;

import java.net.SocketAddress;

/**
 *
 */
public final class UnixDomainSocketAddress extends SocketAddress {

    private final Native.SockAddrUn addr_;
    private final String path_;

    public UnixDomainSocketAddress(String path) {
        Native.SockAddrUn addr = new Native.SockAddrUn();
        addr.setSunPath(path);

        addr_ = addr;
        path_ = path;
    }

    UnixDomainSocketAddress(Native.SockAddrUn addr) {
        addr_ = addr;
        path_ = new String(addr.sun_path_);
    }
    public String getPath() {
        return path_;
    }

    Native.SockAddrUn addr() {
        return addr_;
    }
}
