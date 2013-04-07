package net.ihiroky.niotty.buffer;

/**
 * @author Hiroki Itoh
 */
public final class CodecUtil {

    private CodecUtil() {
        throw new AssertionError();
    }

    static final int BYTE_MASK = 0xFF;
    static final int SHORT_MASK = 0xFFFF;
    static final long INT_MASK = 0xFFFFFFFFL;
    static final int CHAR_BYTES = Character.SIZE / Byte.SIZE;
    static final int SHORT_BYTES = Short.SIZE / Byte.SIZE;
    static final int INT_BYTES = Integer.SIZE / Byte.SIZE;
    static final int LONG_BYTES = Long.SIZE / Byte.SIZE;
    static final int BITS_PER_BYTE = Byte.SIZE;
    static final int BYTE_SHIFT1 = BITS_PER_BYTE;
    static final int BYTE_SHIFT2 = BYTE_SHIFT1 + BITS_PER_BYTE;
    static final int BYTE_SHIFT3 = BYTE_SHIFT2 + BITS_PER_BYTE;
    static final int BYTE_SHIFT4 = BYTE_SHIFT3 + BITS_PER_BYTE;
    static final int BYTE_SHIFT5 = BYTE_SHIFT4 + BITS_PER_BYTE;
    static final int BYTE_SHIFT6 = BYTE_SHIFT5 + BITS_PER_BYTE;
    static final int BYTE_SHIFT7 = BYTE_SHIFT6 + BITS_PER_BYTE;

    static final int VB_BIT_IN_FIRST_BYTE = 6;
    static final int VB_BIT_IN_BYTE = 7;
    static final int VB_MASK_BIT6 = 0x3F;
    static final int VB_MASK_BIT7 = 0x7F;
    static final int VB_END_BIT = 0x80;
    static final int VB_SIGN_BIT = 0x40;
    static final int VB_LONG_MIN_LAST = 0x02;
    static final int VB_INT_MIN_LAST = 0x10;
    static final long VB_BIT32 = 0xFFFFFFFFL;

    static int variableByteLength(int value) {
        if (value == Integer.MIN_VALUE) return 5;

        int magnitude = (value >= 0) ? value : -value;
        int bits = VB_BIT_IN_FIRST_BYTE;
        if (magnitude < (1 << bits)) return 1; // 6 bits

        bits += VB_BIT_IN_BYTE;
        if (magnitude < (1 << bits)) return 2; // 13 bits

        bits += VB_BIT_IN_BYTE;
        if (magnitude < (1 << bits)) return 3; // 20 bits

        bits += VB_BIT_IN_BYTE;
        if (magnitude < (1 << bits)) return 4; // 27 bits

        return 5;
    }
}
