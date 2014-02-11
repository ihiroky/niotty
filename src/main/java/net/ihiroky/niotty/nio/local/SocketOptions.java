package net.ihiroky.niotty.nio.local;

import java.net.SocketOption;

/**
 *
 */
public class SocketOptions {

    public static final SocketOption<Boolean> SO_PASSCRED =
            new SocketOptionImpl<Boolean>("SO_PASSCRED", Boolean.class);

    private static class SocketOptionImpl<T> implements SocketOption<T> {

        final String name_;
        final Class<T> type_;

        SocketOptionImpl(String name, Class<T> type) {
            name_ = name;
            type_ = type;
        }

        @Override
        public String name() {
            return null;
        }

        @Override
        public Class<T> type() {
            return null;
        }
    }
}
