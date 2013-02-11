package net.ihiroky.niotty.buffer;

import java.nio.ByteBuffer;

/**
 * @author Hiroki Itoh
 */
public class ByteBufferDecodeBuffer implements DecodeBuffer {

    private ByteBuffer byteBuffer;

    private static final ByteBuffer EMPTY_BUFFER = ByteBuffer.allocate(0);
    private static final int MINIMUM_GROWTH = 1024;

    ByteBufferDecodeBuffer() {
        this.byteBuffer = EMPTY_BUFFER;
    }

    private ByteBufferDecodeBuffer(ByteBuffer byteBuffer) {
        this.byteBuffer = byteBuffer;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int readByte() {
        return byteBuffer.get() & CodecUtil.BYTE_MASK;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readBytes(byte[] bytes, int offset, int length) {
        byteBuffer.get(bytes, offset, length);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int readBytes4(int bytes) {
        if (bytes < 0 || bytes > CodecUtil.INT_BYTES) {
            throw new IllegalArgumentException("bytes must be in [0, 4].");
        }
        return (int) readBytes8(bytes);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long readBytes8(int bytes) {
        if (bytes < 0 || bytes > CodecUtil.LONG_BYTES) {
            throw new IllegalArgumentException("bytes must be in [0, 8].");
        }
        return readBytes8NoRangeCheck(bytes);
    }

    private long readBytes8NoRangeCheck(int bytes) {
        long decoded = 0L;
        for (int i = 0; i < bytes; i++) {
            decoded = (decoded << CodecUtil.BITS_PER_BYTE) | (byteBuffer.get() & CodecUtil.BYTE_MASK);
        }
        return decoded;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public char readChar() {
        return byteBuffer.getChar();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public short readShort() {
        return byteBuffer.getShort();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int readInt() {
        return byteBuffer.getInt();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long readLong() {
        return byteBuffer.getLong();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public float readFloat() {
        return Float.intBitsToFloat(byteBuffer.getInt());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double readDouble() {
        return Double.longBitsToDouble(byteBuffer.getLong());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int remainingBytes() {
        return byteBuffer.remaining();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int capacityBytes() {
        return byteBuffer.capacity();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void reset() {
        // ready to drainFrom()
        byteBuffer.clear();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BufferSink toBufferSink() {
        return new ByteBufferBufferSink(byteBuffer);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ByteBuffer toByteBuffer() {
        return byteBuffer.asReadOnlyBuffer();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void drainFrom(DecodeBuffer decodeBuffer) {
        ByteBuffer bb = byteBuffer;
        ByteBuffer input = decodeBuffer.toByteBuffer();
        int space = bb.capacity() - bb.limit();
        int remaining = input.remaining();
        if (space < remaining) {
            int required = bb.capacity() + remaining;
            ByteBuffer newBuffer = ByteBuffer.allocate((required >= MINIMUM_GROWTH) ? required : MINIMUM_GROWTH);
            newBuffer.put(bb).flip();
            byteBuffer = newBuffer;
            bb = newBuffer;
        }
        int limit = bb.limit();
        bb.limit(limit + remaining);
        bb.position(limit);
        bb.put(input);
        bb.position(0);
    }

    public static ByteBufferDecodeBuffer wrap(ByteBuffer bb) {
        return new ByteBufferDecodeBuffer(bb);
    }
}
