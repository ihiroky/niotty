package net.ihiroky.niotty.nio.unix;

import java.io.IOException;
import java.nio.channels.NetworkChannel;
import java.nio.channels.spi.AbstractSelectableChannel;

/**
 *
 *
 * An instance of this class accept {@link UnixDomainSocketAddress} only.
 * Implementations throw ClassCastException on attempts to use any operations.
 */
public abstract class AbstractChannel extends AbstractSelectableChannel implements NetworkChannel {

    protected final int fd_;
    protected final Object stateLock_;
    private final int validOps_;

    protected AbstractChannel(int fd, int validOps) throws IOException {
        super(null);

        fd_ = fd;
        validOps_ = validOps;
        stateLock_ = new Object();
    }

    @Override
    protected void implCloseSelectableChannel() throws IOException {
        synchronized (stateLock_) {
            if (Native.shutdown(fd_, Native.SHUT_RDWR) == -1) {
                throw new IOException(Native.getLastError());
            }
            if (Native.close(fd_) == -1) {
                throw new IOException(Native.getLastError());
            }
        }
    }

    @Override
    protected void implConfigureBlocking(boolean block) throws IOException {
        synchronized (stateLock_) {
            int flags = Native.fcntl(fd_, Native.F_GETFL, 0);
            if (block) {
                flags |= Native.O_NONBLOCK;
            } else {
                flags &= ~Native.O_NONBLOCK;
            }
            Native.fcntl(fd_, Native.F_SETFL, flags);
        }
    }

    @Override
    public int validOps() {
        return validOps_;
    }

    @Override
    public String toString() {
        return "{" + fd_ + ", " + (isOpen() ? "open" : "closed") + "}";
    }
}
