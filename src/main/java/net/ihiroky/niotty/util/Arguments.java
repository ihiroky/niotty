package net.ihiroky.niotty.util;

/**
 * Provides utility methods for checking arguments of a method.
 */
public final class Arguments {

    private Arguments() {
        throw new AssertionError();
    }

    /**
     * Checks if the value is not null.
     * @param value the value to check
     * @param name the name of the variable
     * @param <T> the type of the value
     * @return the value
     * @throws NullPointerException if the value is null
     */
    public static <T> T requireNonNull(T value, String name) {
        if (value == null) {
            throw (name != null)
                    ? new NullPointerException("The " + name + " must not be null.")
                    : new NullPointerException();
        }
        return value;
    }

    /**
     * Checks if the int value is positive.
     * @param value the value to check
     * @param name the name of the variable
     * @return the value
     * @throws IllegalArgumentException if the value is not positive
     */
    public static int requirePositive(int value, String name) {
        if (value <= 0) {
            throw (name != null)
                    ? new IllegalArgumentException("The " + name + " must be positive.")
                    : new IllegalArgumentException();
        }
        return value;
    }

    /**
     * Checks if the long value is positive.
     * @param value the value to check
     * @param name the name of the variable
     * @return the value
     * @throws IllegalArgumentException if the value is not positive
     */
    public static long requirePositive(long value, String name) {
        if (value <= 0L) {
            throw (name != null)
                    ? new IllegalArgumentException("The " + name + " must be positive.")
                    : new IllegalArgumentException();
        }
        return value;
    }

    /**
     * Checks if the int value is positive or zero.
     * @param value the value to check
     * @param name the name of the variable
     * @return the value
     * @throws IllegalArgumentException if the value is negative
     */
    public static int requirePositiveOrZero(int value, String name) {
        if (value < 0) {
            throw (name != null)
                    ? new IllegalArgumentException("The " + name + " must be positive or zero.")
                    : new IllegalArgumentException();
        }
        return value;
    }

    /**
     * Checks if the long value is positive or zero.
     * @param value the value to check
     * @param name the name of the variable
     * @return the value
     * @throws IllegalArgumentException if the value is negative
     */
    public static long requirePositiveOrZero(long value, String name) {
        if (value < 0L) {
            throw (name != null)
                    ? new IllegalArgumentException("The " + name + " must be positive or zero.")
                    : new IllegalArgumentException();
        }
        return value;
    }

    /**
     * Checks if the int value is in the specified range.
     *
     * @param value the value to check
     * @param name the name of the variable
     * @param min the minimum value of the range
     * @param max the maximum value of the range
     * @return the value
     */
    public static int requireInRange(int value, String name, int min, int max) {
        if (value < min || value > max) {
            throw (name != null)
                    ? new IllegalArgumentException("The " + name + " must be in [" + min + ", " + max + "].")
                    : new IllegalArgumentException();
        }
        return value;
    }

    /**
     * Checks if the long value is in the specified range.
     *
     * @param value the value to check
     * @param name the name of the variable
     * @param min the minimum value of the range
     * @param max the maximum value of the range
     * @return the value
     */
    public static long requireInRange(long value, String name, long min, long max) {
        if (value < min || value > max) {
            throw (name != null)
                    ? new IllegalArgumentException("The " + name + " must be in [" + min + ", " + max + "].")
                    : new IllegalArgumentException();
        }
        return value;
    }
}
