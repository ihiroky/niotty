package net.ihiroky.niotty.buffer;

/**
 * A skeletal implementation of {@link net.ihiroky.niotty.buffer.CodecBuffer}.
 * @author Hiroki Itoh
 */
public abstract class AbstractCodecBuffer implements CodecBuffer {

    /** A factor to expand internal buffer. */
    protected static final int EXPAND_MULTIPLIER = 2;

    @Override
    public int readUnsignedByte() {
        return readByte() & CodecUtil.BYTE_MASK;
    }

    @Override
    public int readUnsignedShort() {
        return readShort() & CodecUtil.SHORT_MASK;
    }

    @Override
    public int readMedium() {
        int value = readUnsignedMedium();
        return ((value & CodecUtil.MEDIUM_SIGN_MASK) != 0)
                ? (value | CodecUtil.MEDIUM_UPPER8_MASK) : value;
    }

    @Override
    public long readUnsignedInt() {
        return readInt() & CodecUtil.INT_MASK;
    }

    /**
     * Writes {@code Long.MIN_VALUE} with signed VBC.
     */
    private void writeVBLongMinValue() {
        // write sign bit and 0 on 6 bits
        writeByte(CodecUtil.VB_SIGN_BIT);

        // write 0 on 54 bits
        writeByte(0);
        writeByte(0);
        writeByte(0);
        writeByte(0);
        writeByte(0);
        writeByte(0);
        writeByte(0);
        writeByte(0);

        // write last bit with end bit and negative sign bit
        writeByte(CodecUtil.VB_END_BIT | CodecUtil.VB_LONG_MIN_LAST);
    }

    /**
     * Writes {@code Long.MIN_VALUE} with signed VBC.
     */
    private void writeVBIntMinValue() {
        // write sign bit and 0 on 6 bits
        writeByte(CodecUtil.VB_SIGN_BIT);

        // write 0 on 21 bits
        writeByte(0);
        writeByte(0);
        writeByte(0);

        // write last bit with end bit and negative sign bit
        writeByte(CodecUtil.VB_END_BIT | CodecUtil.VB_INT_MIN_LAST);
    }

    /**
     * {@inheritDoc}
     */
    public CodecBuffer writeVariableByteNull() {
        return writeByte(CodecUtil.VB_END_BIT | CodecUtil.VB_SIGN_BIT);
    }

    /**
     * {@inheritDoc}
     */
    public CodecBuffer writeVariableByteLong(long value) {

        // Long.MIN_VALUE overflows if calculate it using ordinary logic.
        if (value == Long.MIN_VALUE) {
            writeVBLongMinValue();
            return this;
        }

        boolean isPositiveOrZero = (value >= 0);
        long magnitude = isPositiveOrZero ? value : -value; // negative zero is null see #

        if (magnitude <= CodecUtil.VB_MASK_BIT6) { // end with one byte
            return writeByte((int) magnitude | (isPositiveOrZero ? 0 : CodecUtil.VB_SIGN_BIT) | CodecUtil.VB_END_BIT);
        }

        writeByte((int) magnitude & CodecUtil.VB_MASK_BIT6 | (isPositiveOrZero ? 0 : CodecUtil.VB_SIGN_BIT));
        magnitude >>>= CodecUtil.VB_BIT_IN_FIRST_BYTE;
        for ( ; magnitude > CodecUtil.VB_MASK_BIT7; magnitude >>>= CodecUtil.VB_BIT_IN_BYTE) {
            writeByte((int) magnitude & CodecUtil.VB_MASK_BIT7);
        }
        return writeByte((int) magnitude | CodecUtil.VB_END_BIT);
    }

    /**
     * {@inheritDoc}
     */
    public CodecBuffer writeVariableByteInteger(int value) {

        // Integer.MIN_VALUE overflows if calculate it using ordinary logic.
        if (value == Integer.MIN_VALUE) {
            writeVBIntMinValue();
            return this;
        }

        boolean isPositiveOrZero = (value >= 0);
        int magnitude = isPositiveOrZero ? value : -value; // negative zero is null see #

        if (magnitude <= CodecUtil.VB_MASK_BIT6) { // end with one byte
            return writeByte(magnitude | (isPositiveOrZero ? 0 : CodecUtil.VB_SIGN_BIT) | CodecUtil.VB_END_BIT);
        }

        writeByte(magnitude & CodecUtil.VB_MASK_BIT6 | (isPositiveOrZero ? 0 : CodecUtil.VB_SIGN_BIT));
        magnitude >>>= CodecUtil.VB_BIT_IN_FIRST_BYTE;
        for ( ; magnitude > CodecUtil.VB_MASK_BIT7; magnitude >>>= CodecUtil.VB_BIT_IN_BYTE) {
            writeByte(magnitude & CodecUtil.VB_MASK_BIT7);
        }
        return writeByte(magnitude | CodecUtil.VB_END_BIT);
    }

    /**
     * {@inheritDoc}
     */
    public CodecBuffer writeVariableByteInteger(Integer value) {
        if (value == null) {
            writeVariableByteNull();
            return this;
        }
        return writeVariableByteInteger(value.intValue());
    }

    /**
     * {@inheritDoc}
     */
    public CodecBuffer writeVariableByteLong(Long value) {
        if (value == null) {
            writeVariableByteNull();
            return this;
        }
        return writeVariableByteLong(value.longValue());
    }

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
        int intValue = (int) value;
        return (intValue == value) ? intValue : value;
    }

    /**
     * {@inheritDoc}
     */
    public long readVariableByteLong() {
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
     * {@inheritDoc}
     */
    public int readVariableByteInteger() {
        int b = readByte();
        if ((b & CodecUtil.VB_END_BIT) != 0) {
            boolean isPositiveOrZero = (b & CodecUtil.VB_SIGN_BIT) == 0;
            // return (negative) zero if data is null
            int magnitude = b & CodecUtil.VB_MASK_BIT6;
            return isPositiveOrZero ? magnitude : -magnitude;
        }
        return readTrailingVariableByteInt(b);
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
            value |= (long) (b & CodecUtil.VB_MASK_BIT7) << shift;
            shift += CodecUtil.VB_BIT_IN_BYTE;
        }
        // -Long.MIN_VALUE == Long.MIN_VALUE
        value |= (long) (b & CodecUtil.VB_MASK_BIT7) << shift;
        return isPositiveOrZero ? value : -value;
    }

    /**
     * Reads {@code long} value in signed VBC from second byte. First byte of signed VBC is given as {@code firstByte}
     * of the argument.
     * @param firstByte first byte of signed VBC
     * @return {@code long} value
     */
    private int readTrailingVariableByteInt(int firstByte) {
        int b = firstByte;
        boolean isPositiveOrZero = (b & CodecUtil.VB_SIGN_BIT) == 0;
        int value = b & CodecUtil.VB_MASK_BIT6;
        int shift = CodecUtil.VB_BIT_IN_FIRST_BYTE;
        for (;;) {
            b = readByte();
            if ((b & CodecUtil.VB_END_BIT) != 0) {
                break;
            }
            value |= (b & CodecUtil.VB_MASK_BIT7) << shift;
            shift += CodecUtil.VB_BIT_IN_BYTE;
        }
        // -Integer.MIN_VALUE == Integer.MIN_VALUE
        value |= (b & CodecUtil.VB_MASK_BIT7) << shift;
        return isPositiveOrZero ? value : -value;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CodecBuffer writeFloat(float value) {
        return writeInt(Float.floatToIntBits(value));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CodecBuffer writeDouble(double value) {
        return writeLong(Double.doubleToLongBits(value));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public float readFloat() {
        return Float.intBitsToFloat(readInt());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double readDouble() {
        return Double.longBitsToDouble(readLong());
    }
}
