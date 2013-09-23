package net.ihiroky.niotty.util;

import java.nio.charset.Charset;

/**
 * Provides constants of {@code java.nio.charset.Charset}.
 */
public final class Charsets {

    /** Shift_JIS. */
    public static final Charset SHIFT_JIS = Charset.forName("Shift_JIS");

    /** EUC_JP. */
    public static final Charset EUC_JP = Charset.forName("EUC_JP");

    /** ISO_8859_1. */
    public static final Charset ISO_8859_1 = Charset.forName("ISO_8859_1");

    /** US-ASCII. */
    public static final Charset US_ASCII = Charset.forName("US-ASCII");

    /** UTF-8. */
    public static final Charset UTF_8 = Charset.forName("UTF-8");

    private Charsets() {
        throw new AssertionError();
    }
}
