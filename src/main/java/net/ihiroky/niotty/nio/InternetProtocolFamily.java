package net.ihiroky.niotty.nio;

import java.net.ProtocolFamily;
import java.net.StandardProtocolFamily;

/**
 *
 */
public enum InternetProtocolFamily {
    INET4,
    INET6,
    ;

    public static ProtocolFamily resolve(InternetProtocolFamily family) {
        switch (family) {
            case INET4: return StandardProtocolFamily.INET;
            case INET6: return StandardProtocolFamily.INET6;
            default: throw new UnsupportedOperationException();
        }
    }
}
