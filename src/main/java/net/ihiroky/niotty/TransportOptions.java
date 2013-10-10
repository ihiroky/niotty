package net.ihiroky.niotty;

import java.net.NetworkInterface;

/**
 *
 */
public final class TransportOptions {

    public static final TransportOption<Boolean> SO_BROADCAST =
            new TransportOptionImpl<Boolean>("SO_BROADCAST", Boolean.class);
    public static final TransportOption<Boolean> SO_KEEPALIVE =
            new TransportOptionImpl<Boolean>("SO_KEEPALIVE", Boolean.class);
    public static final TransportOption<Integer> SO_LINGER =
            new TransportOptionImpl<Integer>("SO_LINGER", Integer.class);
    public static final TransportOption<Integer> SO_RCVBUF =
            new TransportOptionImpl<Integer>("SO_RCVBUF", Integer.class);
    public static final TransportOption<Boolean> SO_REUSEADDR =
            new TransportOptionImpl<Boolean>("SO_REUSEADDR", Boolean.class);
    public static final TransportOption<Integer> SO_SNDBUF =
            new TransportOptionImpl<Integer>("SO_SNDBUF", Integer.class);
    public static final TransportOption<Boolean> TCP_NODELAY =
            new TransportOptionImpl<Boolean>("TCP_NODELAY", Boolean.class);
    public static final TransportOption<NetworkInterface> IP_MULTICAST_IF =
            new TransportOptionImpl<NetworkInterface>("IP_MULTICAST_IF", NetworkInterface.class);
    public static final TransportOption<Boolean> IP_MULTICAST_LOOP =
            new TransportOptionImpl<Boolean>("IP_MULTICAST_LOOP", Boolean.class);
    public static final TransportOption<Integer> IP_MULTICAST_TTL =
            new TransportOptionImpl<Integer>("IP_MULTICAST_TTL", Integer.class);
    public static final TransportOption<Integer> IP_TOS =
            new TransportOptionImpl<Integer>("IP_TOS", Integer.class);

    private static class TransportOptionImpl<T> implements TransportOption<T> {
        private final String name_;
        private final Class<T> type_;

        private TransportOptionImpl(String name, Class<T> type) {
            name_ = name;
            type_ = type;
        }

        @Override
        public T cast(Object value) {
            return type_.cast(value);
        }

        @Override
        public String toString() {
            return name_;
        }
    }

    private TransportOptions() {
        throw new AssertionError();
    }
}
