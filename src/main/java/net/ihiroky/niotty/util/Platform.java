package net.ihiroky.niotty.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.nio.ch.DirectBuffer;

import java.nio.ByteBuffer;

/**
 * Java version etc.
 */
public final class Platform {

    private static Logger logger_ = LoggerFactory.getLogger(Platform.class);

    private static final JavaVersion JAVA_VERSION;

    static {
        JAVA_VERSION = resolveJavaVersion();
    }

    public static JavaVersion javaVersion() {
        return JAVA_VERSION;
    }

    private static JavaVersion resolveJavaVersion() {
        String version = System.getProperty("java.version");
        String vendor = System.getProperty("java.vendor");
        if (version.startsWith("1.7")) {
            return JavaVersion.JAVA7;
        } else if (version.startsWith("1.6")) {
            return JavaVersion.JAVA6;
        }

        try {
            Class.forName("android.Manifest");
            return JavaVersion.JAVA6;
        } catch (Throwable ignored) {
        }
        throw new AssertionError("Invalid version/vendor " + version + "/" + vendor);
    }

    public static void release(ByteBuffer buffer) {
        if (buffer.isDirect()) {
            try {
                if (buffer instanceof DirectBuffer) {
                    ((DirectBuffer) buffer).cleaner().clean();
                }
            } catch (Throwable t) {
                logger_.warn("[release]", t);
            }
        }
    }
}
