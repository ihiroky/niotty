package net.ihiroky.niotty.buffer;

import net.ihiroky.niotty.util.Charsets;

import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;

/**
 * A skeletal implementation of {@link CodecBuffer}.
 * @author Hiroki Itoh
 */
public abstract class AbstractCodecBuffer extends AbstractPacket implements CodecBuffer {

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


    private static final byte[] MIN_LONG_ASCII = new byte[] {
            '-', '9', '2', '2', '3', '3', '7', '2', '0', '3', '6', '8', '5', '4', '7', '7', '5', '8', '0', '8'
    };

    @Override
    public CodecBuffer writeLongAsAscii(long value) {
        if (value == Long.MIN_VALUE) {
            writeBytes(MIN_LONG_ASCII, 0, MIN_LONG_ASCII.length);
            return this;
        }

        int size = (value < 0) ? stringSize(-value) + 1 : stringSize(value);
        writeLongAsAscii10(value, size);
        return this;
    }

    @Override
    public long readLongAsAscii(int length) {
        if (length <= 0 || length > MIN_LONG_ASCII.length || remaining() < length) {
            throw (length <= MIN_LONG_ASCII.length)
                    ? new IllegalArgumentException("Invalid length: " + length + ", remaining: " + remaining())
                    : new IllegalArgumentException("The length must be less than or equal " + MIN_LONG_ASCII.length);
        }

        boolean negative = false;
        int head = readByte();
        int i = 1;
        if (head < '0') { // Possible leading "+" or "-"
            if (head == '-') {
                negative = true;
            } else if (head != '+') {
                String restString = readStringContent(Charsets.US_ASCII.newDecoder(), length - 1);
                throw new NumberFormatException(((char) head) + restString);
            }

            // Cannot have lone "+" or "-"
            if (length == 1) {
                throw new NumberFormatException(Character.toString((char) head));
            }
            head = readByte();
            i++;
        }

        long result = 0;
        int digit = head - '0';
        long mulMin;
        mulMin = Long.MAX_VALUE / 10;
        for (;;) {
            if (digit < 0 || digit > 9) {
                throw newNumberFormatException(i, length);
            }
            if (result > mulMin) {
                throw newNumberFormatException(i, length);
            }
            result = (result << 3) + (result << 1);
            result += digit;

            if (i == length) {
                break;
            }
            digit = readByte() - '0';
            i++;
        }

        if (result < 0) {
            if (!negative) { // Over Long.MAX_VALUE
                throw newNumberFormatException(i, length);
            }
            if (result != Long.MIN_VALUE) { // Under long.MIN_VALUE
                throw newNumberFormatException(i, length);
            }
        }

        // The result gets Long.MIN_VALUE if input is MIN_LONG_ASCII by overflow,
        // but -Long.MIN_VALUE == Long.MIN_VALUE.
        return negative ? -result : result;
    }

    private NumberFormatException newNumberFormatException(int skip, int bytes) {
        skipStartIndex(-skip);
        String string = readStringContent(Charsets.US_ASCII.newDecoder(), bytes);
        throw new NumberFormatException(string);
    }

    // Requires positive x
    private static int stringSize(long x) {
        long p = 10;
        for (int i = 1; i < 19; i++) {
            if (x < p) {
                return i;
            }
            // p = p * 10;
            p = (p << 3) + (p << 1);
        }
        return 19;
    }

    private final static byte[] DigitTens = {
            '0', '0', '0', '0', '0', '0', '0', '0', '0', '0',
            '1', '1', '1', '1', '1', '1', '1', '1', '1', '1',
            '2', '2', '2', '2', '2', '2', '2', '2', '2', '2',
            '3', '3', '3', '3', '3', '3', '3', '3', '3', '3',
            '4', '4', '4', '4', '4', '4', '4', '4', '4', '4',
            '5', '5', '5', '5', '5', '5', '5', '5', '5', '5',
            '6', '6', '6', '6', '6', '6', '6', '6', '6', '6',
            '7', '7', '7', '7', '7', '7', '7', '7', '7', '7',
            '8', '8', '8', '8', '8', '8', '8', '8', '8', '8',
            '9', '9', '9', '9', '9', '9', '9', '9', '9', '9',
    } ;

    private final static byte[] DigitOnes = {
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
    } ;

    private final static byte[] digits = {
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9'
    };

    /*
     * Places characters representing the integer i into the
     * character array buf. The characters are placed into
     * the buffer backwards starting with the least significant
     * digit at the specified index (exclusive), and working
     * backwards from there.
     *
     * Will fail if i == Long.MIN_VALUE
     */
    private void writeLongAsAscii10(long i, int size) {
        long q;
        int r;
        byte sign = 0;
        int charPos = size;
        byte[] buf = new byte[size];

        if (i < 0) {
            sign = '-';
            i = -i;
        }

        // Get 2 digits/iteration using longs until quotient fits into an int
        while (i > Integer.MAX_VALUE) {
            q = i / 100;
            // really: r = i - (q * 100);
            r = (int)(i - ((q << 6) + (q << 5) + (q << 2)));
            i = q;
            buf[--charPos] = DigitOnes[r];
            buf[--charPos] = DigitTens[r];
        }

        // Get 2 digits/iteration using ints
        int q2;
        int i2 = (int)i;
        while (i2 >= 65536) {
            q2 = i2 / 100;
            // really: r = i2 - (q * 100);
            r = i2 - ((q2 << 6) + (q2 << 5) + (q2 << 2));
            i2 = q2;
            buf[--charPos] = DigitOnes[r];
            buf[--charPos] = DigitTens[r];
        }

        // Fall thru to fast mode for smaller numbers
        // assert(i2 <= 65536, i2);
        for (;;) {
            q2 = (i2 * 52429) >>> (16 + 3); // q2 = i2 / 10 if (i2 <= 81919)
            r = i2 - ((q2 << 3) + (q2 << 1));  // r = i2-(q2*10) ...
            buf[--charPos] = digits[r];
            i2 = q2;
            if (i2 == 0) {
                break;
            }
        }

        if (sign != 0) {
            buf[--charPos] = sign;
        }

        writeBytes(buf, 0, buf.length);
    }

    @Override
    public CodecBuffer writeString(String s, CharsetEncoder encoder) {
        int length = s.length();
        if (length == 0) {
            writeVariableByteInteger(0);
            return this;
        }

        // Assume that encoder.maxBytesPerChar() (at most 6) bytes per character.
        int expectedLengthBytes = CodecUtil.variableByteLength((int) (length * encoder.maxBytesPerChar()));
        int e = skipEndIndex(expectedLengthBytes);
        if (e < expectedLengthBytes) {
            writeVBIntMinValue(); // Write 5 bytes to ensure capacity.
            skipEndIndex(expectedLengthBytes - 5 - e);
        }
        int e0 = endIndex();
        writeStringContent(s, encoder);
        int e1 = endIndex();
        endIndex(e0 - expectedLengthBytes);
        int actualLength = e1 - e0;
        int actualLengthBytes = CodecUtil.variableByteLength(actualLength);
        if (expectedLengthBytes == actualLengthBytes) {
            writeVariableByteInteger(actualLength);
        } else {
            writePaddedVariableBytePositive(actualLength);
        }
        endIndex(e1);
        return this;
    }

    private void writePaddedVariableBytePositive(int value) {
        if (value <= CodecUtil.VB_MASK_BIT6) {
            writeByte(value);
            writeByte(CodecUtil.VB_END_BIT);
            return;
        }

        writeByte(value & CodecUtil.VB_MASK_BIT6);
        value >>>= CodecUtil.VB_BIT_IN_FIRST_BYTE;
        for (; value > CodecUtil.VB_MASK_BIT7; value >>>= CodecUtil.VB_BIT_IN_BYTE) {
            writeByte(value & CodecUtil.VB_MASK_BIT7);
        }
        writeByte(value);
        writeByte(CodecUtil.VB_END_BIT);
    }

    @Override
    public String readString(CharsetDecoder decoder) {
        int length = readVariableByteInteger();
        return readStringContent(decoder, length);
    }
}
