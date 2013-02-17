package net.ihiroky.niotty.buffer;

/**
 * A skeletal implementation of {@link net.ihiroky.niotty.buffer.DecodeBuffer}.
 *
 * @author Hiroki Itoh
 */
public abstract class AbstractDecodeBuffer implements DecodeBuffer {

    /**
     * {@inheritDoc}
     */
    public Number readVariableByteNumber() {
        int b = readByte();
        if ((b & CodecUtil.VB_END_BIT) != 0) {
            boolean isPositiveOrZero = (b & CodecUtil.VB_SIGN_BIT) == 0;
            int magnitude = b & CodecUtil.VB_MASK_BIT6;
            if (isPositiveOrZero) {
                return magnitude;
            }
            return (magnitude == 0) ? null : -magnitude;
        }
        long value = readTrailingVariableByte(b);
        boolean within32Bits = ((value & CodecUtil.VB_BIT32) == value);
        return within32Bits ? (int) value : value;
    }

    /**
     * {@inheritDoc}
     */
    public long readVariableByte() {
        int b = readByte();
        if ((b & CodecUtil.VB_END_BIT) != 0) {
            boolean isPositiveOrZero = (b & CodecUtil.VB_SIGN_BIT) == 0;
            // return (negative) zero if data is null
            int magnitude = b & CodecUtil.VB_MASK_BIT6;
            return isPositiveOrZero ? magnitude : -magnitude;
        }
        return readTrailingVariableByte(b);
    }

    /**
     * Reads {@code long} value in signed VBC from second byte. First byte of signed VBC is given as {@code firstByte}
     * of the argument.
     * @param firstByte first byte of signed VBC
     * @return {@code long} value
     */
    private long readTrailingVariableByte(int firstByte) {
        int b = firstByte;
        boolean isPositiveOrZero = (b & CodecUtil.VB_SIGN_BIT) == 0;
        long value = b & CodecUtil.VB_MASK_BIT6;
        int shift = CodecUtil.VB_BIT_IN_FIRST_BYTE;
        for (;;) {
            b = readByte();
            if ((b & CodecUtil.VB_END_BIT) != 0) {
                break;
            }
            value |= (b & CodecUtil.VB_MASK_BIT7) << shift;
            shift += CodecUtil.VB_BIT_IN_BYTE;
        }
        // if the value is 0 and and the b is 0x02 with sign bit, then Long.MIN_VALUE
        if (value == 0 && b == CodecUtil.VB_LONG_MIN_LAST) {
            return Long.MIN_VALUE;
        }
        value |= (b & CodecUtil.VB_MASK_BIT7) << shift;
        return isPositiveOrZero ? value : -value;
    }

}
