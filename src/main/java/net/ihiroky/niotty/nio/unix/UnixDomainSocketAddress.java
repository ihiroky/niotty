package net.ihiroky.niotty.nio.unix;

import java.net.SocketAddress;

/**
 *
 */
public final class UnixDomainSocketAddress extends SocketAddress {

    private final String path_;

    public UnixDomainSocketAddress(String path) {
        path_ = path;
    }

    public String getPath() {
        return path_;
    }

    @Override
    public String toString() {
        return path_;
    }
}
