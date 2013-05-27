package net.ihiroky.niotty;

import java.net.SocketAddress;

/**
 * A default implementation of {@link net.ihiroky.niotty.TransportParameter}.
 *
 * @author Hiroki Itoh
 */
public class DefaultTransportParameter implements TransportParameter {

    private SocketAddress address_;
    private int priority_;

    /** Default priority (no wait). */
    private static final int DEFAULT_PRIORITY = -1;

    private static final int HASH_BASE = 17;
    private static final int HASH_FACTOR = 31;

    public static final DefaultTransportParameter NO_PARAMETER = new DefaultTransportParameter(DEFAULT_PRIORITY);

    /**
     * Creates a new instance.
     * @param address a target or source address.
     */
    public DefaultTransportParameter(SocketAddress address) {
        this(address, DEFAULT_PRIORITY);
    }

    /**
     * Creates a new instance.
     * @param priority a priority to control write operation.
     */
    public DefaultTransportParameter(int priority) {
        this(null, priority);
    }

    /**
     * Creates a new instance.
     * @param address a target or source address.
     * @param priority a priority to control a write operation.
     */
    public DefaultTransportParameter(SocketAddress address, int priority) {
        address_ = address;
        priority_ = priority;
    }

    @Override
    public int priority() {
        return priority_;
    }

    @Override
    public Object attachment() {
        return address_;
    }

    @Override
    public int hashCode() {
        int h = HASH_BASE;
        h = h * HASH_FACTOR + priority_;
        h = h * HASH_FACTOR + ((address_ != null) ? address_.hashCode() : 0);
        return h;
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof DefaultTransportParameter) {
            DefaultTransportParameter that = (DefaultTransportParameter) object;
            return this.priority_ == that.priority_
                    && ((this.address_ != null) ? this.address_.equals(that.address_) : that.address_ == null);
        }
        return false;
    }

    @Override
    public String toString() {
        return "address: " + address_ + ", priority: " + priority_;
    }
}
