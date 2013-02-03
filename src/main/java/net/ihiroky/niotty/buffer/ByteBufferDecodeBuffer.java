package net.ihiroky.niotty.buffer;

import java.nio.ByteBuffer;

/**
 * @author Hiroki Itoh
 */
public class ByteBufferDecodeBuffer implements DecodeBuffer {

    private ByteBuffer byteBuffer;

    ByteBufferDecodeBuffer(ByteBuffer byteBuffer) {
        this.byteBuffer = byteBuffer;
    }

    @Override
    public int readByte() {
        return byteBuffer.get() & CodecUtil.BYTE_MASK;
    }

    @Override
    public int readBytes(byte[] bytes, int offset, int length) {
        byteBuffer.get(bytes, offset, length);
        return length;
    }

    @Override
    public int readBytes4(int bytes) {
        return (int) readBytes8(bytes);
    }

    @Override
    public long readBytes8(int bytes) {
        long decoded = 0L;
        for (int i = 0; i < bytes; i++) {
            decoded = (decoded << CodecUtil.BITS_PER_BYTE) | (byteBuffer.get() & CodecUtil.BYTE_MASK);
        }
        return decoded;
    }

    @Override
    public char readChar() {
        return byteBuffer.getChar();
    }

    @Override
    public short readShort() {
        return byteBuffer.getShort();
    }

    @Override
    public int readInt() {
        return byteBuffer.getInt();
    }

    @Override
    public long readLong() {
        return byteBuffer.getLong();
    }

    @Override
    public float readFloat() {
        return Float.intBitsToFloat(byteBuffer.getInt());
    }

    @Override
    public double readDouble() {
        return Double.longBitsToDouble(byteBuffer.getLong());
    }

    @Override
    public int leftBytes() {
        return byteBuffer.remaining();
    }

    @Override
    public int wholeBytes() {
        return byteBuffer.limit();
    }

    @Override
    public void clear() {
        byteBuffer.clear();
    }

    @Override
    public BufferSink toBufferSink() {
        return new ByteBufferBufferSink(byteBuffer);
    }

    @Override
    public ByteBuffer toByteBuffer() {
        return byteBuffer.asReadOnlyBuffer();
    }

    @Override
    public void transferFrom(ByteBuffer buffer) {
        byteBuffer = buffer;
    }
}
