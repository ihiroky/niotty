package net.ihiroky.niotty.buffer;

/**
 * A skeletal implementation of {@link net.ihiroky.niotty.buffer.EncodeBuffer}.
 * @author Hiroki Itoh
 */
public abstract class AbstractEncodeBuffer implements EncodeBuffer {

    /**
     * Ensures the backed store capacity. The new capacity is the large of the two, sum of the current position
     * and {@code length}, and twice the size of current capacity.
     *
     * @param length the size of byte to be written
     */
    abstract void ensureSpace(int length);

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
        writeByte(CodecUtil.VB_LONG_MIN_LAST);
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
        writeByte(CodecUtil.VB_INT_MIN_LAST);
    }

    /**
     * {@inheritDoc}
     */
    public void writeVariableByteNull() {
        writeByte(CodecUtil.VB_END_BIT | CodecUtil.VB_SIGN_BIT);
    }

    /**
     * {@inheritDoc}
     */
    public void writeVariableByteLong(long value) {

        // Long.MIN_VALUE overflows if calculate it using ordinary logic.
        if (value == Long.MIN_VALUE) {
            writeVBLongMinValue();
            return;
        }

        boolean isPositiveOrZero = (value >= 0);
        long magnitude = isPositiveOrZero ? value : -value; // negative zero is null see #

        if (magnitude <= CodecUtil.VB_MASK_BIT6) { // end with one byte
            ensureSpace(1);
            writeByte((int) magnitude | (isPositiveOrZero ? 0 : CodecUtil.VB_SIGN_BIT) | CodecUtil.VB_END_BIT);
            return;
        }

        ensureSpace(CodecUtil.VB_LONGEST_LONG_BYTE);
        writeByte((int) magnitude & CodecUtil.VB_MASK_BIT6 | (isPositiveOrZero ? 0 : CodecUtil.VB_SIGN_BIT));
        magnitude >>>= CodecUtil.VB_BIT_IN_FIRST_BYTE;
        for ( ; magnitude > CodecUtil.VB_MASK_BIT7; magnitude >>>= CodecUtil.VB_BIT_IN_BYTE) {
            writeByte((int) magnitude & CodecUtil.VB_MASK_BIT7);
        }
        writeByte((int) magnitude | CodecUtil.VB_END_BIT);
    }

    public void writeVariableByteInteger(int value) {

        // Integer.MIN_VALUE overflows if calculate it using ordinary logic.
        if (value == Integer.MIN_VALUE) {
            writeVBIntMinValue();
            return;
        }

        boolean isPositiveOrZero = (value >= 0);
        int magnitude = isPositiveOrZero ? value : -value; // negative zero is null see #

        if (magnitude <= CodecUtil.VB_MASK_BIT6) { // end with one byte
            ensureSpace(1);
            writeByte(magnitude | (isPositiveOrZero ? 0 : CodecUtil.VB_SIGN_BIT) | CodecUtil.VB_END_BIT);
            return;
        }

        ensureSpace(CodecUtil.VB_LONGEST_INT_BYTE);
        writeByte(magnitude & CodecUtil.VB_MASK_BIT6 | (isPositiveOrZero ? 0 : CodecUtil.VB_SIGN_BIT));
        magnitude >>>= CodecUtil.VB_BIT_IN_FIRST_BYTE;
        for ( ; magnitude > CodecUtil.VB_MASK_BIT7; magnitude >>>= CodecUtil.VB_BIT_IN_BYTE) {
            writeByte(magnitude & CodecUtil.VB_MASK_BIT7);
        }
        writeByte(magnitude | CodecUtil.VB_END_BIT);
    }

    /**
     * {@inheritDoc}
     */
    public void writeVariableByteInteger(Integer value) {
        if (value == null) {
            writeVariableByteNull();
            return;
        }
        writeVariableByteInteger(value.intValue());
    }

    /**
     * {@inheritDoc}
     */
    public void writeVariableByteLong(Long value) {
        if (value == null) {
            writeVariableByteNull();
            return;
        }
        writeVariableByteLong(value.longValue());
    }
}
