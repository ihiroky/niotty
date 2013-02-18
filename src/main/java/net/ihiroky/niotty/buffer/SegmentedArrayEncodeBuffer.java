package net.ihiroky.niotty.buffer;

import java.nio.charset.CharsetEncoder;

/**
 * A buffer to encode primitive and byte array values.
 * A byte array to store encoded bytes increase its capacity as needed.
 * The byte order is little endian.
 *
 * @author Hiroki Itoh
 */
public class SegmentedArrayEncodeBuffer extends AbstractEncodeBuffer implements EncodeBuffer {

    private byte[][] segments;
    private int segmentIndex;
    private int countInSegment;
    private final int segmentLength;

    private static final int DEFAULT_BANK_LENGTH = 1024;
    private static final int BANKS_PER_GROW = 8;

    public SegmentedArrayEncodeBuffer() {
        this(DEFAULT_BANK_LENGTH);
    }

    public SegmentedArrayEncodeBuffer(int segmentLength) {
        if (segmentLength < 16) {
            throw new IllegalArgumentException("segmentLength must be ge 16");
        }

        byte[][] banks = new byte[BANKS_PER_GROW][];
        banks[0] = new byte[segmentLength];
        this.segments = banks;
        this.segmentLength = segmentLength;
    }

    private void proceedSegmentIfInBankLast() {
        if (countInSegment == segmentLength) {
            segmentIndex++;
            countInSegment = 0;
        }
    }

    @Override
    public void writeByte(int value) {
        ensureSpace(1);
        segments[segmentIndex][countInSegment++] = (byte) value;
        proceedSegmentIfInBankLast();
    }

    @Override
    public void writeBytes(byte[] bytes, int offset, int length) {
        int si = segmentIndex;
        int cis = countInSegment;
        int space = segmentLength - cis;
        if (space >= length) {
            System.arraycopy(bytes, offset, segments[si], cis, length);
            countInSegment += length;
            proceedSegmentIfInBankLast();
            return;
        }

        // write to current and next bank
        ensureSpace(length);
        System.arraycopy(bytes, offset, segments[si++], cis, space);
        int leftOffset = offset + space;
        int leftBytes = length - space;
        space = segmentLength;
        while (leftBytes > space) {
            System.arraycopy(bytes, leftOffset, segments[si++], 0, space);
            leftOffset += space;
            leftBytes -= space;
        }
        System.arraycopy(bytes, leftOffset, segments[si], 0, leftBytes);
        segmentIndex = si;
        countInSegment = leftBytes;
    }

    @Override
    public void writeBytes4(int bits, int bytes) {
        writeBytes8(bits & 0xFFFFFFFFL, bytes);
    }

    @Override
    public void writeBytes8(long bits, int bytes) {
        int cis = countInSegment;
        int space = segmentLength - cis;
        if (space >= bytes) {
            byte[] segment = segments[segmentIndex];
            int bytesMinus1 = bytes - 1;
            for (int i = 0; i < bytes; i++) {
                segment[cis + i] = (byte) ((bits >>> (bytesMinus1 - i)) & CodecUtil.BYTE_MASK);
            }
            countInSegment += bytes;
            proceedSegmentIfInBankLast();
            return;
        }
        writeBytes8AcrossBanks(bits, bytes, space);
    }

    private void writeBytes8AcrossBanks(long bits, int bytes, int space) {
        ensureSpace(bytes);
        int si = segmentIndex;
        int cis = countInSegment;
        int bytesMinus1 = bytes - 1;
        byte[] segment = segments[si];
        int i = 0;
        for (; i < space; i++) {
            segment[cis + i] = (byte) ((bits >>> (bytesMinus1 - i)) & CodecUtil.BYTE_MASK);
        }
        segment = segments[++si];
        for (; i< bytes; i++) {
            segment[i - space] = (byte) ((bits >>> (bytesMinus1 - i)) & CodecUtil.BYTE_MASK);
        }
        segmentIndex = si;
        countInSegment = bytes - space;
    }

    @Override
    public void writeChar(char value) {
        int si = segmentIndex;
        int cis = countInSegment;
        int space = segmentLength - cis;
        if (space >= CodecUtil.SHORT_BYTES) {
            byte[] segment = segments[si];
            segment[cis    ] = (byte) (value >>> CodecUtil.BYTE_SHIFT1);
            segment[cis + 1] = (byte) (value & CodecUtil.BYTE_MASK);
            proceedSegmentIfInBankLast();
            return;
        }

        ensureSpace(CodecUtil.CHAR_BYTES);
        segments[si][cis] = (byte) (value >>> CodecUtil.BYTE_SHIFT1);
        segments[si + 1][0] = (byte) (value & CodecUtil.BYTE_MASK);
        segmentIndex = si + 1;
        countInSegment = 1;
    }

    @Override
    public void writeShort(short value) {
        int si = segmentIndex;
        int cis = countInSegment;
        int space = segmentLength - cis;
        if (space >= CodecUtil.SHORT_BYTES) {
            byte[] segment = segments[si];
            segment[cis    ] = (byte) (value >>> CodecUtil.BYTE_SHIFT1);
            segment[cis + 1] = (byte) (value & CodecUtil.BYTE_MASK);
            proceedSegmentIfInBankLast();
            return;
        }

        ensureSpace(CodecUtil.SHORT_BYTES);
        segments[si][cis] = (byte) (value >>> CodecUtil.BYTE_SHIFT1);
        segments[si + 1][0] = (byte) (value & CodecUtil.BYTE_MASK);
        segmentIndex = si + 1;
        countInSegment = 1;
    }

    @Override
    public void writeInt(int value) {
        int cis = countInSegment;
        int space = segmentLength - cis;
        if (space >= CodecUtil.INT_BYTES) {
            byte[] segment = segments[segmentIndex];
            segment[cis    ] = (byte) (value >>> CodecUtil.BYTE_SHIFT3);
            segment[cis + 1] = (byte) ((value >>> CodecUtil.BYTE_SHIFT2) & CodecUtil.BYTE_MASK);
            segment[cis + 2] = (byte) ((value >>> CodecUtil.BYTE_SHIFT1) & CodecUtil.BYTE_MASK);
            segment[cis + 3] = (byte) (value & CodecUtil.BYTE_MASK);
            proceedSegmentIfInBankLast();
            return;
        }
        writeBytes8AcrossBanks(value & 0xFFFFFFFFL, CodecUtil.INT_BYTES, space);
    }

    @Override
    public void writeLong(long value) {
        int cis = countInSegment;
        int space = segmentLength - cis;
        if (space >= CodecUtil.LONG_BYTES) {
            byte[] segment = segments[segmentIndex];
            segment[cis    ] = (byte) (value >>> CodecUtil.BYTE_SHIFT7);
            segment[cis + 1] = (byte) ((value >>> CodecUtil.BYTE_SHIFT6) & CodecUtil.BYTE_MASK);
            segment[cis + 2] = (byte) ((value >>> CodecUtil.BYTE_SHIFT5) & CodecUtil.BYTE_MASK);
            segment[cis + 3] = (byte) ((value >>> CodecUtil.BYTE_SHIFT4) & CodecUtil.BYTE_MASK);
            segment[cis + 4] = (byte) ((value >>> CodecUtil.BYTE_SHIFT3) & CodecUtil.BYTE_MASK);
            segment[cis + 5] = (byte) ((value >>> CodecUtil.BYTE_SHIFT2) & CodecUtil.BYTE_MASK);
            segment[cis + 6] = (byte) ((value >>> CodecUtil.BYTE_SHIFT1) & CodecUtil.BYTE_MASK);
            segment[cis + 7] = (byte) (value & CodecUtil.BYTE_MASK);
            proceedSegmentIfInBankLast();
            return;
        }
        writeBytes8AcrossBanks(value, CodecUtil.LONG_BYTES, space);
    }

    @Override
    public void writeFloat(float value) {
        writeInt(Float.floatToIntBits(value));
    }

    @Override
    public void writeDouble(double value) {
        writeLong(Double.doubleToLongBits(value));
    }

    @Override
    public void writeString(CharsetEncoder charsetEncoder, String s) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void drainTo(EncodeBuffer encodeBuffer) {
        int si1 = segmentIndex - 1;
        for (int i = 0; i < si1; i++) {
            encodeBuffer.writeBytes(segments[i], 0, segmentLength);
        }
        encodeBuffer.writeBytes(segments[segmentIndex], 0, countInSegment);
        segmentIndex = 0;
        countInSegment = 0;
    }

    @Override
    void ensureSpace(int bytes) {
        int space = segmentLength - countInSegment;
        if (bytes <= space) {
            return;
        }

        byte[][] ss = segments;
        int requiredBytes = bytes - space;
        for (int currentSegment = segmentIndex; requiredBytes > 0; requiredBytes -= segmentLength) {
            if (ss.length == currentSegment) {
                byte[][] t = new byte[currentSegment + BANKS_PER_GROW][];
                System.arraycopy(ss, 0, t, 0, ss.length);
                ss = t;
            }
            ss[++currentSegment] = new byte[segmentLength];
        }
        segments = ss;
    }

    @Override
    public int filledBytes() {
        return segmentIndex * segmentLength + countInSegment;
    }

    @Override
    public int capacityBytes() {
        return segments.length * segmentLength;
    }

    @Override
    public void clear() {
        segmentIndex = 0;
        countInSegment = 0;
    }

    @Override
    public BufferSink createBufferSink() {
        return new SegmentedArrayBufferSink(segments, segmentIndex, countInSegment);
    }
}
