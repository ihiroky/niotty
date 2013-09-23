package net.ihiroky.niotty.util;

/**
 * Java version etc.
 */
public final class Platform {

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
}
