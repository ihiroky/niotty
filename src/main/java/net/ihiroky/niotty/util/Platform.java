package net.ihiroky.niotty.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.misc.Unsafe;
import sun.nio.ch.DirectBuffer;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;

/**
 * Java version etc.
 */
public final class Platform {

    private static Logger logger_ = LoggerFactory.getLogger(Platform.class);

    private static final JavaVersion JAVA_VERSION;
    public static final Unsafe UNSAFE;

    static {

        JAVA_VERSION = resolveJavaVersion();

        try {
            // Hotspot, J9 and Dalvik have sun.misc.Unsafe
            Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            Unsafe unsafe = (Unsafe) theUnsafe.get(null);
            UNSAFE = unsafe;
        } catch (Throwable t) {
            throw new ExceptionInInitializerError(t);
        }
    }

    public static JavaVersion javaVersion() {
        return JAVA_VERSION;
    }

    private static JavaVersion resolveJavaVersion() {
        String version = System.getProperty("java.version");
        String vendor = System.getProperty("java.vendor");
        if (version.startsWith("1.9.")) {
            return JavaVersion.JAVA9;
        } else if (version.startsWith("1.8.")) {
            return JavaVersion.JAVA8;
        } else if (version.startsWith("1.7.")) {
            return JavaVersion.JAVA7;
        } else if (version.startsWith("1.6.")) {
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
