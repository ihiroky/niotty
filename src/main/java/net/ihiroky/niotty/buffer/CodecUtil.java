package net.ihiroky.niotty.buffer;

/**
 * @author Hiroki Itoh
 */
public final class CodecUtil {

    private CodecUtil() {
        throw new AssertionError();
    }

    static final int BYTE_MASK = 0xFF;
    static final int CHAR_BYTES = 2;
    static final int SHORT_BYTES = 2;
    static final int INT_BYTES = 4;
    static final int LONG_BYTES = 8;
    static final int BITS_PER_BYTE = 8;
    static final int BYTE_SHIFT1 = BITS_PER_BYTE;
    static final int BYTE_SHIFT2 = BYTE_SHIFT1 + BITS_PER_BYTE;
    static final int BYTE_SHIFT3 = BYTE_SHIFT2 + BITS_PER_BYTE;
    static final int BYTE_SHIFT4 = BYTE_SHIFT3 + BITS_PER_BYTE;
    static final int BYTE_SHIFT5 = BYTE_SHIFT4 + BITS_PER_BYTE;
    static final int BYTE_SHIFT6 = BYTE_SHIFT5 + BITS_PER_BYTE;
    static final int BYTE_SHIFT7 = BYTE_SHIFT6 + BITS_PER_BYTE;
}
