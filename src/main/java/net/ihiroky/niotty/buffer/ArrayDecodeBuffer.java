package net.ihiroky.niotty.buffer;

import java.nio.ByteBuffer;

/**
 * Created on 13/02/01, 15:22
 *
 * @author Hiroki Itoh
 */
public class ArrayDecodeBuffer implements DecodeBuffer {

    private byte[] buffer;
    private int position;

    @Override
    public int readByte() {
        return buffer[position++] & CodecUtil.BYTE_MASK;
    }

    @Override
    public int readBytes(byte[] bytes, int offset, int length) {
        System.arraycopy(buffer, position, bytes, offset, length);
        position += length;
        return length;
    }

    @Override
    public int readBytes4(int bytes) {
        return (int) readBytes8(bytes);
    }

    @Override
    public long readBytes8(int bytes) {
        long decoded = 0L;
        byte[] b = buffer;
        int pos = position;
        for (int i = 0; i < bytes; i++) {
            decoded = (decoded << CodecUtil.BITS_PER_BYTE) | (b[pos + i] & CodecUtil.BYTE_MASK);
        }
        position += bytes;
        return decoded;
    }

    @Override
    public char readChar() {
        byte[] b = buffer;
        int pos = position;
        return (char) (((b[pos] & CodecUtil.BYTE_MASK) << CodecUtil.BYTE_SHIFT1)
                | (b[pos + 1] & CodecUtil.BYTE_MASK));
    }

    @Override
    public short readShort() {
        byte[] b = buffer;
        int pos = position;
        return (short) (((b[pos] & CodecUtil.BYTE_MASK) << CodecUtil.BYTE_SHIFT1)
                | (b[pos + 1] & CodecUtil.BYTE_MASK));
    }

    @Override
    public int readInt() {
        byte[] b = buffer;
        int pos = position;
        return  (((b[pos + 3] & CodecUtil.BYTE_MASK) << CodecUtil.BYTE_SHIFT3)
                | ((b[pos + 2] & CodecUtil.BYTE_MASK) << CodecUtil.BYTE_SHIFT2)
                | ((b[pos + 1] & CodecUtil.BYTE_MASK) << CodecUtil.BYTE_SHIFT1)
                | (b[pos + 1] & CodecUtil.BYTE_MASK));
    }

    @Override
    public long readLong() {
        byte[] b = buffer;
        int pos = position;
        return  ((((long) b[pos] & CodecUtil.BYTE_MASK) << CodecUtil.BYTE_SHIFT7)
                | (((long) b[pos + 1] & CodecUtil.BYTE_MASK) << CodecUtil.BYTE_SHIFT6)
                | (((long) b[pos + 2] & CodecUtil.BYTE_MASK) << CodecUtil.BYTE_SHIFT5)
                | (((long) b[pos + 3] & CodecUtil.BYTE_MASK) << CodecUtil.BYTE_SHIFT4)
                | (((long) b[pos + 4] & CodecUtil.BYTE_MASK) << CodecUtil.BYTE_SHIFT3)
                | (((long) b[pos + 5] & CodecUtil.BYTE_MASK) << CodecUtil.BYTE_SHIFT2)
                | (((long) b[pos + 6] & CodecUtil.BYTE_MASK) << CodecUtil.BYTE_SHIFT1)
                | ((long) b[pos + 7] & CodecUtil.BYTE_MASK));
    }

    @Override
    public float readFloat() {
        return Float.intBitsToFloat(readInt());
    }

    @Override
    public void clear() {
        position = 0;
    }

    @Override
    public double readDouble() {
        return Double.longBitsToDouble(readLong());
    }

    @Override
    public int leftBytes() {
        return buffer.length - position;
    }

    @Override
    public int wholeBytes() {
        return buffer.length;
    }

    @Override
    public BufferSink toBufferSink() {
        return new ArrayBufferSink(buffer);
    }

    @Override
    public void transferFrom(ByteBuffer byteBuffer) {
        position = 0;
        if (byteBuffer.hasArray() && byteBuffer.arrayOffset() == 0) {
            this.buffer = byteBuffer.array();
            return;
        }
        int length = byteBuffer.remaining();
        byte[] b = new byte[length];
        byteBuffer.get(b, 0, length);
        this.buffer = b;
    }
}
