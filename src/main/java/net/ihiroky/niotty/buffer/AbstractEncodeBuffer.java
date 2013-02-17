package net.ihiroky.niotty.buffer;

/**
 * A skeletal implementation of {@link net.ihiroky.niotty.buffer.EncodeBuffer}.
 * @author Hiroki Itoh
 */
public abstract class AbstractEncodeBuffer implements EncodeBuffer {

    /**
     * Writes {@code b} to this buffer without capacity check.
     * @param b byte data to be written
     */
    abstract void writeByteNoCheck(byte b);

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

        ensureSpace(CodecUtil.VB_LONGEST_BYTE);

        // write sign bit and 0 on 6 bits
        writeByteNoCheck((byte) CodecUtil.VB_SIGN_BIT);
        // write 0 on 54 bits
        for (int i = 0; i < CodecUtil.LONG_BYTES; i++) {
            writeByteNoCheck((byte) 0);
        }
        // write last bit with end bit and negative sign bit
        writeByteNoCheck((byte) CodecUtil.VB_LONG_MIN_LAST);
    }

    /**
     * {@inheritDoc}
     */
    public void writeVariableByteNull() {
        ensureSpace(1);
        writeByteNoCheck((byte) (CodecUtil.VB_END_BIT | CodecUtil.VB_SIGN_BIT));
    }

    /**
     * {@inheritDoc}
     */
    public void writeVariableByte(long value) {

        // Long.MIN_VALUE overflows if calculate ordinary logic.
        if (value == Long.MIN_VALUE) {
            writeVBLongMinValue();
            return;
        }

        boolean isPositiveOrZero = (value >= 0);
        long magnitude = isPositiveOrZero ? value : -value; // negative zero is null see #

        if (magnitude <= CodecUtil.VB_MASK_BIT6) { // end with one byte
            ensureSpace(1);
            writeByteNoCheck((byte)
                    (magnitude | (isPositiveOrZero ? 0 : CodecUtil.VB_SIGN_BIT) | CodecUtil.VB_END_BIT));
            return;
        }

        ensureSpace(CodecUtil.VB_LONGEST_BYTE);
        writeByteNoCheck((byte) (magnitude & CodecUtil.VB_MASK_BIT6 | (isPositiveOrZero ? 0 : CodecUtil.VB_SIGN_BIT)));
        magnitude >>>= CodecUtil.VB_BIT_IN_FIRST_BYTE;
        for ( ; magnitude > CodecUtil.VB_MASK_BIT7; magnitude >>>= CodecUtil.VB_BIT_IN_BYTE) {
            writeByteNoCheck((byte) (magnitude & CodecUtil.VB_MASK_BIT7));
        }
        writeByteNoCheck((byte) (magnitude | CodecUtil.VB_END_BIT));
    }

    /**
     * {@inheritDoc}
     */
    public void writeVariableByteInteger(Integer value) {
        if (value == null) {
            writeVariableByteNull();
            return;
        }
        writeVariableByte(value.longValue());
    }

    /**
     * {@inheritDoc}
     */
    public void writeVariableByteLong(Long value) {
        if (value == null) {
            writeVariableByteNull();
            return;
        }
        writeVariableByte(value);
    }
}
