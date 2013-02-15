package net.ihiroky.niotty.buffer;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.util.Arrays;

/**
 * Implementation of {@link net.ihiroky.niotty.buffer.DecodeBuffer} using {@code byte[]}.
 *
 * @author Hiroki Itoh
 */
public class ArrayDecodeBuffer implements DecodeBuffer {

    private byte[] buffer;
    private int position;
    private int end;

    private static final byte[] EMPTY_BYTES = new byte[0];

    ArrayDecodeBuffer() {
        buffer = EMPTY_BYTES;
    }

    private ArrayDecodeBuffer(byte[] b, int length) {
        buffer = b;
        end = length;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int readByte() {
        if (position >= end) {
            throw new IndexOutOfBoundsException("position exceeds end of buffer.");
        }
        return buffer[position++] & CodecUtil.BYTE_MASK;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readBytes(byte[] bytes, int offset, int length) {
        System.arraycopy(buffer, position, bytes, offset, length);
        position += length;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readBytes(ByteBuffer byteBuffer) {
        int space = byteBuffer.remaining();
        int remaining = remainingBytes();
        int read = (space <= remaining) ? space : remaining;
        byteBuffer.put(buffer, position, read);
        position += read;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int readBytes4(int bytes) {
        if (bytes < 0 || bytes > CodecUtil.INT_BYTES) {
            throw new IllegalArgumentException("bytes must be in [0, 4].");
        }

        int pos = position;
        byte[] b = buffer;
        int decoded = 0;
        for (int i = 0; i < bytes; i++) {
            decoded = (decoded << CodecUtil.BITS_PER_BYTE) | (b[pos++] & CodecUtil.BYTE_MASK);
        }
        position = pos;
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

        int pos = position;
        byte[] b = buffer;
        long decoded = 0L;
        for (int i = 0; i < bytes; i++) {
            decoded = (decoded << CodecUtil.BITS_PER_BYTE) | (b[pos++] & CodecUtil.BYTE_MASK);
        }
        position = pos;
        return decoded;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public char readChar() {
        if (position + CodecUtil.CHAR_BYTES >= end) {
            throw new IndexOutOfBoundsException("position exceeds the end of buffer if read char.");
        }
        return (char) (((buffer[position++] & CodecUtil.BYTE_MASK) << CodecUtil.BYTE_SHIFT1)
                | (buffer[position++] & CodecUtil.BYTE_MASK));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public short readShort() {
        if (position + CodecUtil.SHORT_BYTES >= end) {
            throw new IndexOutOfBoundsException("position exceeds the end of buffer if read short.");
        }
        return (short) (((buffer[position++] & CodecUtil.BYTE_MASK) << CodecUtil.BYTE_SHIFT1)
                | (buffer[position++] & CodecUtil.BYTE_MASK));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int readInt() {
        byte[] b = buffer;
        int pos = position;
        if (pos + CodecUtil.INT_BYTES >= end) {
            throw new IndexOutOfBoundsException("position exceeds the end of buffer if read int wide byte.");
        }
        int result = (((b[pos++] & CodecUtil.BYTE_MASK) << CodecUtil.BYTE_SHIFT3)
                | ((b[pos++] & CodecUtil.BYTE_MASK) << CodecUtil.BYTE_SHIFT2)
                | ((b[pos++] & CodecUtil.BYTE_MASK) << CodecUtil.BYTE_SHIFT1)
                |  (b[pos++] & CodecUtil.BYTE_MASK));
        position = pos;
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long readLong() {
        byte[] b = buffer;
        int pos = position;
        if (pos + CodecUtil.LONG_BYTES >= end) {
            throw new IndexOutOfBoundsException("position exceeds the end of buffer if read long wide byte.");
        }
        long result = ((((long) b[pos++] & CodecUtil.BYTE_MASK) << CodecUtil.BYTE_SHIFT7)
                | (((long) b[pos++] & CodecUtil.BYTE_MASK) << CodecUtil.BYTE_SHIFT6)
                | (((long) b[pos++] & CodecUtil.BYTE_MASK) << CodecUtil.BYTE_SHIFT5)
                | (((long) b[pos++] & CodecUtil.BYTE_MASK) << CodecUtil.BYTE_SHIFT4)
                | (((long) b[pos++] & CodecUtil.BYTE_MASK) << CodecUtil.BYTE_SHIFT3)
                | (((long) b[pos++] & CodecUtil.BYTE_MASK) << CodecUtil.BYTE_SHIFT2)
                | (((long) b[pos++] & CodecUtil.BYTE_MASK) << CodecUtil.BYTE_SHIFT1)
                |  ((long) b[pos++] & CodecUtil.BYTE_MASK));
        position = pos;
        return result;
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

    /**
     * {@inheritDoc}
     */
    public String readString(CharsetDecoder charsetDecoder, int bytes) {
        String cached = StringCache.getCachedValue(this, charsetDecoder, bytes);
        if (cached != null) {
            return cached;
        }

        float charsPerByte = charsetDecoder.averageCharsPerByte();
        ByteBuffer input = ByteBuffer.wrap(buffer, position, position + bytes);
        CharBuffer output = CharBuffer.allocate(Buffers.outputCharBufferSize(charsPerByte, bytes));
        for (;;) {
            CoderResult cr = charsetDecoder.decode(input, output, true);
            if (!cr.isError() && !cr.isOverflow()) {
                position = input.position();
                break;
            }
            if (cr.isOverflow()) {
                output = Buffers.expand(output, charsPerByte, input.remaining());
                continue;
            }
            if (cr.isError()) {
                position = input.position();
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
        int pos = position;
        int n = end - pos; // remaining
        if (bytes < n) {
            n = (bytes < -pos) ? -pos : bytes;
        }
        position += n;
        return n;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int remainingBytes() {
        return end - position;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int capacityBytes() {
        return buffer.length;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void reset() {
        position = 0;
        end = 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BufferSink toBufferSink() {
        return new ArrayBufferSink(buffer, position, end);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ByteBuffer toByteBuffer() {
        return ByteBuffer.wrap(buffer, position, end);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void drainFrom(DecodeBuffer decodeBuffer) {
        int capacity = buffer.length;
        int space = capacity - end;
        int remaining = decodeBuffer.remainingBytes();
        if (space < remaining) {
            int required = end + remaining;
            int twice = capacity * 2;
            buffer = Arrays.copyOf(buffer, (required >= twice) ? required : twice);
        }
        decodeBuffer.readBytes(buffer, end, remaining);
        end += remaining;
    }

    /**
     * Wraps a specified byte array into {@code ArrayDecodeBuffer}.
     *
     * The new {@code ArrayDecodeBuffer} is backed by the specified byte array. If some data is written into the
     * {@code ArrayDecodeBuffer}, then the backed byte array is also modified and vice versa. The new
     * {@code ArrayDecodeBuffer}'s capacity and end is {@code length} and position is {@code 0}.
     *
     * @param b the backed byte array
     * @param length the capacity and end, must be non-negative and less than or equal to {@code b.length}
     * @return the new {@code ArrayDecodeBuffer}
     */
    public static ArrayDecodeBuffer wrap(byte[] b, int length) {
        return new ArrayDecodeBuffer(b, length);
    }
}
