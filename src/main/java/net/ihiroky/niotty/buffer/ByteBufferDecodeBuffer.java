package net.ihiroky.niotty.buffer;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;

/**
 * @author Hiroki Itoh
 */
public class ByteBufferDecodeBuffer extends AbstractDecodeBuffer implements DecodeBuffer {

    private ByteBuffer buffer;

    ByteBufferDecodeBuffer() {
        ByteBuffer b = ByteBuffer.allocate(512);
        b.limit(0);
        this.buffer = b;
    }

    ByteBufferDecodeBuffer(ByteBuffer buffer) {
        this.buffer = buffer;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int readByte() {
        return buffer.get() & CodecUtil.BYTE_MASK;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readBytes(byte[] bytes, int offset, int length) {
        buffer.get(bytes, offset, length);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readBytes(ByteBuffer byteBuffer) {
        ByteBuffer myBuffer = buffer;
        int space = byteBuffer.remaining();
        if (space >= myBuffer.remaining()) {
            byteBuffer.put(myBuffer);
            return;
        }
        int limit = myBuffer.limit();
        myBuffer.limit(myBuffer.position() + space);
        byteBuffer.put(myBuffer);
        myBuffer.limit(limit);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int readBytes4(int bytes) {
        if (bytes < 0 || bytes > CodecUtil.INT_BYTES) {
            throw new IllegalArgumentException("bytes must be in [0, 4].");
        }
        int decoded = 0;
        for (int i = 0; i < bytes; i++) {
            decoded = (decoded << CodecUtil.BITS_PER_BYTE) | (buffer.get() & CodecUtil.BYTE_MASK);
        }
        return decoded;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long readBytes8(int bytes) {
        if (bytes < 0 || bytes > CodecUtil.LONG_BYTES) {
            throw new IllegalArgumentException("bytes must be in [0, 8].");
        }
        long decoded = 0L;
        for (int i = 0; i < bytes; i++) {
            decoded = (decoded << CodecUtil.BITS_PER_BYTE) | (buffer.get() & CodecUtil.BYTE_MASK);
        }
        return decoded;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public char readChar() {
        return buffer.getChar();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public short readShort() {
        return buffer.getShort();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int readInt() {
        return buffer.getInt();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long readLong() {
        return buffer.getLong();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public float readFloat() {
        return Float.intBitsToFloat(buffer.getInt());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double readDouble() {
        return Double.longBitsToDouble(buffer.getLong());
    }

    @Override
    public String readString(CharsetDecoder charsetDecoder, int bytes) {
        String cached = StringCache.getCachedValue(this, charsetDecoder, bytes);
        if (cached != null) {
            return cached;
        }

        float charsPerByte = charsetDecoder.maxCharsPerByte();
        ByteBuffer input = buffer;
        CharBuffer output = CharBuffer.allocate(Buffers.outputCharBufferSize(charsPerByte, bytes));
        int limit = input.limit();
        input.limit(input.position() + bytes);
        for (;;) {
            CoderResult cr = charsetDecoder.decode(input, output, true);
            if (!cr.isError() && !cr.isOverflow()) {
                input.limit(limit);
                break;
            }
            if (cr.isOverflow()) {
                output = Buffers.expand(output, charsPerByte, input.remaining());
                continue;
            }
            if (cr.isError()) {
                input.limit(limit);
                Buffers.throwRuntimeException(cr);
            }
        }
        output.flip();
        return StringCache.toString(output, charsetDecoder);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int skipBytes(int bytes) {
        ByteBuffer b = buffer;
        int n = b.remaining();
        if (bytes < n) {
            n = (bytes < -b.position()) ? -b.position() : bytes;
        }
        b.position(b.position() + n);
        return n;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int remainingBytes() {
        return buffer.remaining();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int limitBytes() {
        return buffer.limit();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clear() {
        // ready to drainFrom()
        buffer.position(0);
        buffer.limit(0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BufferSink toBufferSink() {
        return new ByteBufferBufferSink(buffer);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ByteBuffer toByteBuffer() {
        return buffer;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasArray() {
        return buffer.hasArray();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public byte[] toArray() {
        return buffer.array();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int arrayOffset() {
        return buffer.arrayOffset();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int drainFrom(DecodeBuffer decodeBuffer) {
        return drainFromNoCheck(decodeBuffer, decodeBuffer.remainingBytes());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int drainFrom(DecodeBuffer decodeBuffer, int bytes) {
        if (bytes < 0) {
            throw new IllegalArgumentException("bytes must be >= 0.");
        }
        int remaining = decodeBuffer.remainingBytes();
        return drainFromNoCheck(decodeBuffer, (bytes <= remaining) ? bytes : remaining);
    }

    /**
     * Drains data from {@code input} with specified bytes
     * @param input buffer which contains the data transferred from
     * @param bytes the number of byte to be transferred
     * @return {@code bytes}
     */
    private int drainFromNoCheck(DecodeBuffer input, int bytes) {
        ByteBuffer bb = buffer;
        int space = bb.capacity() - bb.limit();
        if (space < bytes) {
            int required = bb.limit() + bytes;
            int twice = bb.capacity() * 2;
            ByteBuffer newBuffer = ByteBuffer.allocate((required >= twice) ? required : twice);
            newBuffer.put(bb).flip();
            buffer = newBuffer;
            bb = newBuffer;
        }
        int limit = bb.limit();
        int position = bb.position();
        bb.limit(limit + bytes);
        bb.position(limit);
        input.readBytes(bb);
        bb.position(position);
        return bytes;
    }
}
