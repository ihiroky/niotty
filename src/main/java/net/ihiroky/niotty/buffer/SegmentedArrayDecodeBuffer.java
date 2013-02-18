package net.ihiroky.niotty.buffer;

import java.nio.ByteBuffer;
import java.nio.charset.CharsetDecoder;

/**
 * Created on 13/02/01, 15:22
 *
 * @author Hiroki Itoh
 */
public class SegmentedArrayDecodeBuffer extends AbstractDecodeBuffer implements DecodeBuffer {

    private byte[][] segments;
    private int segmentsIndex;
    private int countInSegment;
    private int lastSegmentIndex;
    private int lastCountInSegment;
    private final int segmentLength;

    private static final int DEFAULT_SEGMENT_LENGTH = 1024;
    private static final int SEGMENTS_PER_GROW = 8;

    SegmentedArrayDecodeBuffer() {
        this(DEFAULT_SEGMENT_LENGTH);
    }

    SegmentedArrayDecodeBuffer(int segmentLength) {
        this.segments = new byte[1][];
        this.segmentLength = segmentLength;
    }

    private void proceedSegmentIfInBankLast() {
        if (countInSegment == segmentLength) {
            segmentsIndex++;
            countInSegment = 0;
        }
    }

    @Override
    public int readByte() {
        int b = segments[segmentsIndex][countInSegment++] & CodecUtil.BYTE_MASK;
        proceedSegmentIfInBankLast();
        return b;
    }

    @Override
    public void readBytes(byte[] bytes, int offset, int length) {
        int si = segmentsIndex;
        int cis = countInSegment;
        int left = segmentLength - cis;
        if (length <= left) {
            System.arraycopy(segments[si], cis, bytes, offset, length);
            countInSegment += length;
            proceedSegmentIfInBankLast();
            return;
        }

        System.arraycopy(segments[si], cis, bytes, offset, left);
        int nextLength = length - left;
        int nextOffset = offset + left;
        if (++si == lastSegmentIndex) {
            readLastSegment(bytes, nextOffset, nextLength);
            return;
        }
        while (nextLength >= segmentLength) {
            System.arraycopy(segments[si], 0, bytes, nextOffset, segmentLength);
            nextOffset += segmentLength;
            nextLength -= segmentLength;
            if (++si == lastSegmentIndex) {
                readLastSegment(bytes, nextOffset, nextLength);
                return;
            }
        }
        System.arraycopy(segments[si], 0, bytes, nextOffset, nextLength);
        segmentsIndex = si;
        countInSegment = nextLength;
    }

    private void readLastSegment(byte[] bytes, int nextOffset, int nextLength) {
        int read = (nextLength <= lastCountInSegment) ? nextLength : lastCountInSegment;
        System.arraycopy(segments[lastSegmentIndex], 0, bytes, nextOffset, read);
        segmentsIndex = lastSegmentIndex;
        countInSegment = read;
    }

    @Override
    public void readBytes(ByteBuffer byteBuffer) {
        int si = segmentsIndex;
        int cis = countInSegment;
        int left = segmentLength - cis;
        int space = byteBuffer.remaining();
        if (space <= left) {
            byteBuffer.put(segments[si], cis, space);
            countInSegment += space;
            proceedSegmentIfInBankLast();
            return;
        }

        byteBuffer.put(segments[si], cis, left);
        if (++si == lastSegmentIndex) {
            readLastSegment(byteBuffer);
            return;
        }
        while (byteBuffer.remaining() >= segmentLength && ++si < lastSegmentIndex) {
            byteBuffer.put(segments[si], 0, segmentLength);
            if (++si == lastSegmentIndex) {
                readLastSegment(byteBuffer);
                return;
            }
        }
        space = byteBuffer.remaining();
        byteBuffer.put(segments[si], 0, space);
        segmentsIndex = si;
        countInSegment = space;
    }

    private void readLastSegment(ByteBuffer byteBuffer) {
        int space = byteBuffer.remaining();
        int read = (space <= lastCountInSegment) ? space : lastCountInSegment;
        byteBuffer.put(segments[lastSegmentIndex], 0, read);
        segmentsIndex = lastSegmentIndex;
        countInSegment = read;
    }

    @Override
    public int readBytes4(int bytes) {
        return (int) readBytes8(bytes);
    }

    @Override
    public long readBytes8(int bytes) {
        int cis = countInSegment;
        int left = segmentLength - cis;
        if (bytes <= left) {
            byte[] segment = segments[segmentsIndex];
            long decoded = 0L;
            for (int i = 0; i < bytes; i++) {
                decoded = (decoded << CodecUtil.BITS_PER_BYTE) | (segment[cis + i] & CodecUtil.BYTE_MASK);
            }
            proceedSegmentIfInBankLast();
            return decoded;
        }
        return readBytes8AcrossBanks(bytes, left);
    }

    // FROM HERE
    private long readBytes8AcrossBanks(int bytes, int left) {
        int si = segmentsIndex;
        int cis = countInSegment;
        byte[] segment = segments[si];
        long decoded = 0L;
        int i = 0;
        for (; i < left; i++) {
            decoded = (decoded << CodecUtil.BITS_PER_BYTE) | (segment[cis + i] & CodecUtil.BYTE_MASK);
        }
        segment = segments[++si];
        for (; i< bytes; i++) {
            decoded = (decoded << CodecUtil.BITS_PER_BYTE) | (segment[i - left] & CodecUtil.BYTE_MASK);
        }
        segmentsIndex = si;
        countInSegment = bytes - left;
        return decoded;
    }

    @Override
    public char readChar() {
        int cis = countInSegment;
        int left = segmentLength - cis;
        if (left >= CodecUtil.CHAR_BYTES) {
            byte[] segment = segments[segmentsIndex];
            char decoded = (char) (((segment[cis] & CodecUtil.BYTE_MASK) << CodecUtil.BYTE_SHIFT1)
                    | (segment[cis + 1] & CodecUtil.BYTE_MASK));
            proceedSegmentIfInBankLast();
            return decoded;
        }
        int si = segmentsIndex;
        char decoded = (char) ((segments[si][cis] & CodecUtil.BYTE_MASK) | (segments[si + 1][0] & CodecUtil.BYTE_MASK));
        segmentsIndex = si + 1;
        countInSegment = 1;
        return decoded;

    }

    @Override
    public short readShort() {
        int cis = countInSegment;
        int left = segmentLength - cis;
        if (left >= CodecUtil.SHORT_BYTES) {
            byte[] segment = segments[segmentsIndex];
            short decoded = (short) (((segment[cis] & CodecUtil.BYTE_MASK) << CodecUtil.BYTE_SHIFT1)
                    | (segment[cis + 1] & CodecUtil.BYTE_MASK));
            proceedSegmentIfInBankLast();
            return decoded;
        }
        int si = segmentsIndex;
        short decoded = (short) ((segments[si][cis] & CodecUtil.BYTE_MASK) | (segments[si + 1][0] & CodecUtil.BYTE_MASK));
        segmentsIndex = si + 1;
        countInSegment = 1;
        return decoded;
    }

    @Override
    public int readInt() {
        int cis = countInSegment;
        int left = segmentLength - cis;
        if (left >= CodecUtil.INT_BYTES) {
            byte[] segment = segments[segmentsIndex];
            int decoded = ((segment[cis] & CodecUtil.BYTE_MASK) << CodecUtil.BYTE_SHIFT3)
                    | ((segment[cis + 1] & CodecUtil.BYTE_MASK) << CodecUtil.BYTE_SHIFT2)
                    | ((segment[cis + 2] & CodecUtil.BYTE_MASK) << CodecUtil.BYTE_SHIFT1)
                    | (segment[cis + 3] & CodecUtil.BYTE_MASK);
            proceedSegmentIfInBankLast();
            return decoded;
        }
        return (int) readBytes8AcrossBanks(CodecUtil.INT_BYTES, left);
    }

    @Override
    public long readLong() {
        int cis = countInSegment;
        int left = segmentLength - cis;
        if (left >= CodecUtil.LONG_BYTES) {
            byte[] segment = segments[segmentsIndex];
            long decoded = (((long) segment[cis] & CodecUtil.BYTE_MASK) << CodecUtil.BYTE_SHIFT7)
                    | (((long) segment[cis + 1] & CodecUtil.BYTE_MASK) << CodecUtil.BYTE_SHIFT6)
                    | (((long) segment[cis + 2] & CodecUtil.BYTE_MASK) << CodecUtil.BYTE_SHIFT5)
                    | (((long) segment[cis + 3] & CodecUtil.BYTE_MASK) << CodecUtil.BYTE_SHIFT4)
                    | (((long) segment[cis + 4] & CodecUtil.BYTE_MASK) << CodecUtil.BYTE_SHIFT3)
                    | (((long) segment[cis + 5] & CodecUtil.BYTE_MASK) << CodecUtil.BYTE_SHIFT2)
                    | (((long) segment[cis + 6] & CodecUtil.BYTE_MASK) << CodecUtil.BYTE_SHIFT1)
                    | ((long) segment[cis + 7] & CodecUtil.BYTE_MASK);
            proceedSegmentIfInBankLast();
            return decoded;
        }
        return readBytes8AcrossBanks(CodecUtil.LONG_BYTES, left);
    }

    @Override
    public float readFloat() {
        return Float.intBitsToFloat(readInt());
    }

    @Override
    public void reset() {
        segmentsIndex = 0;
        countInSegment = 0;
    }

    @Override
    public double readDouble() {
        return Double.longBitsToDouble(readLong());
    }

    @Override
    public String readString(CharsetDecoder charsetDecoder) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int skipBytes(int bytes) {
        int n = remainingBytes();
        int pos = segmentsIndex * segmentLength + countInSegment;
        if (bytes < n) {
            n = (bytes < -pos) ? -pos : bytes;
        }
        int si = segmentsIndex;
        int cib = countInSegment + n;
        while (cib >= segmentLength) {
            si++;
            cib -= segmentLength;
        }
        while (cib < 0) {
            si--;
            cib += segmentLength;
        }
        segmentsIndex = si;
        countInSegment = cib;
        return n;
    }

    @Override
    public int remainingBytes() {
        return segmentLength * (lastSegmentIndex - segmentsIndex) + lastCountInSegment - countInSegment;
    }

    @Override
    public int capacityBytes() {
        return segmentLength * segments.length;
    }

    @Override
    public BufferSink toBufferSink() {
        return new SegmentedArrayBufferSink(segments);
    }

    @Override
    public ByteBuffer toByteBuffer() {
        if (lastSegmentIndex == 0) {
            return ByteBuffer.wrap(segments[0], 0, lastCountInSegment);
        }

        ByteBuffer byteBuffer = ByteBuffer.allocate(capacityBytes());
        for (int i = 0; i < lastSegmentIndex; i++) {
            byteBuffer.put(segments[i], 0, segmentLength);
        }
        byteBuffer.put(segments[lastSegmentIndex], 0, lastCountInSegment);
        return byteBuffer;
    }

    // TODO FileChannel direct transfer ?
    @Override
    public void drainFrom(DecodeBuffer decodeBuffer) {
        byte[][] ss = segments;
        int lsi = lastSegmentIndex;
        int space = segmentLength - lastCountInSegment;
        int read;
        int remaining = 0;
        while ((remaining = decodeBuffer.remainingBytes()) > 0) {
            if (lsi == ss.length) {
                byte[][] t = new byte[ss.length + SEGMENTS_PER_GROW][];
                System.arraycopy(ss, 0, t, 0, ss.length);
                ss = t;
            }
            if (space == 0) {
                ss[++lsi] = new byte[segmentLength];
                space = segmentLength;
            }
            read = (remaining >= space) ? space : remaining;
            decodeBuffer.readBytes(ss[lsi], segmentLength - space, space);
            space -= read;
        }
        segments = ss;
        lastSegmentIndex = lsi;
        lastCountInSegment = remaining;
    }
}
