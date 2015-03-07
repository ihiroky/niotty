package net.ihiroky.niotty.util;

/**
 *
 */
public enum JavaVersion {
    JAVA6(6),
    JAVA7(7),
    JAVA8(8),
    JAVA9(9),
    ;

    private final int version_;

    JavaVersion(int version) {
        version_ = version;
    }

    public boolean ge(JavaVersion that) {
        return this.version_ >= that.version_;
    }

    public boolean gt(JavaVersion that) {
        return this.version_ > that.version_;
    }

    public boolean le(JavaVersion that) {
        return this.version_ <= that.version_;
    }

    public boolean lt(JavaVersion that) {
        return this.version_ < that.version_;
    }

    public void throwIfUnsupported(JavaVersion required) {
        if (lt(required)) {
            throw new UnsupportedOperationException("Java " + required.version_ + " or later is required.");
        }
    }

    @Override
    public String toString() {
        return "Java ".concat(Integer.toString(version_));
    }
}
