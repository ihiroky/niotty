package net.ihiroky.niotty.util;

import java.util.Arrays;

/**
 * Provides utility methods for null-related operation.
 */
public final class Objects {

    private Objects() {
        throw new AssertionError();
    }

    /**
     * Checks if the value is null or not.
     * @param value the value to check
     * @param name the name of the variable
     * @param <T> the type of the value
     * @return the value
     */
    public static <T> T requireNonNull(T value, String name) {
        if (value == null) {
            throw (name != null)
                    ? new NullPointerException(name + " must not be null.")
                    : new NullPointerException();
        }
        return value;
    }

    /**
     * Returns true if the arguments are equal to each other.
     * @param a an object
     * @param b an object
     * @return true if the arguments are equal to each other.
     */
    public static boolean equals(Object a, Object b) {
        return (a != null) ? a.equals(b) : b == null;
    }

    /**
     * Calculate a hash code for the value.
     * @param value the value
     * @return the hash code
     */
    public static int hash(Object value) {
        return (value != null) ? value.hashCode() : 0;
    }

    /**
     * Calculate a hash code for the values.
     * @param values the values
     * @return the hash code
     */
    public static int hash(Object...values) {
        return Arrays.hashCode(values);
    }
}
