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
public class ArrayDecodeBuffer extends AbstractDecodeBuffer implements DecodeBuffer {

    private byte[] buffer_;
    private int offset_;
    private int position_;
    private int end_;

    private static final byte[] EMPTY_BYTES = new byte[0];

    ArrayDecodeBuffer() {
        buffer_ = EMPTY_BYTES;
    }

    ArrayDecodeBuffer(byte[] b, int offset, int length) {
        if (offset + length > b.length) {
            throw new IndexOutOfBoundsException(
                    "offset + length (" + (offset + length) + ") exceeds buffer capacity " + b.length);
        }
        buffer_ = b;
        this.offset_ = offset;
        position_ = offset;
        end_ = offset + length;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int readByte() {
        if (position_ >= end_) {
            throw new IndexOutOfBoundsException("position exceeds end of buffer.");
        }
        return buffer_[position_++] & CodecUtil.BYTE_MASK;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readBytes(byte[] bytes, int offset, int length) {
        System.arraycopy(buffer_, position_, bytes, offset, length);
        position_ += length;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readBytes(ByteBuffer byteBuffer) {
        int space = byteBuffer.remaining();
        int remaining = remainingBytes();
        int read = (space <= remaining) ? space : remaining;
        byteBuffer.put(buffer_, position_, read);
        position_ += read;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int readBytes4(int bytes) {
        if (bytes < 0 || bytes > CodecUtil.INT_BYTES) {
            throw new IllegalArgumentException("bytes must be in [0, 4].");
        }

        int pos = position_;
        byte[] b = buffer_;
        int decoded = 0;
        for (int i = 0; i < bytes; i++) {
            decoded = (decoded << CodecUtil.BITS_PER_BYTE) | (b[pos++] & CodecUtil.BYTE_MASK);
        }
        position_ = pos;
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

        int pos = position_;
        byte[] b = buffer_;
        long decoded = 0L;
        for (int i = 0; i < bytes; i++) {
            decoded = (decoded << CodecUtil.BITS_PER_BYTE) | (b[pos++] & CodecUtil.BYTE_MASK);
        }
        position_ = pos;
        return decoded;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public char readChar() {
        if (position_ + CodecUtil.CHAR_BYTES > end_) {
            throw new IndexOutOfBoundsException("position exceeds the end of buffer if read char.");
        }
        return (char) (((buffer_[position_++] & CodecUtil.BYTE_MASK) << CodecUtil.BYTE_SHIFT1)
                | (buffer_[position_++] & CodecUtil.BYTE_MASK));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public short readShort() {
        if (position_ + CodecUtil.SHORT_BYTES > end_) {
            throw new IndexOutOfBoundsException("position exceeds the end of buffer if read short.");
        }
        return (short) (((buffer_[position_++] & CodecUtil.BYTE_MASK) << CodecUtil.BYTE_SHIFT1)
                | (buffer_[position_++] & CodecUtil.BYTE_MASK));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int readInt() {
        byte[] b = buffer_;
        int pos = position_;
        if (pos + CodecUtil.INT_BYTES > end_) {
            throw new IndexOutOfBoundsException("position exceeds the end of buffer if read int wide byte.");
        }
        int result = (((b[pos++] & CodecUtil.BYTE_MASK) << CodecUtil.BYTE_SHIFT3)
                | ((b[pos++] & CodecUtil.BYTE_MASK) << CodecUtil.BYTE_SHIFT2)
                | ((b[pos++] & CodecUtil.BYTE_MASK) << CodecUtil.BYTE_SHIFT1)
                |  (b[pos++] & CodecUtil.BYTE_MASK));
        position_ = pos;
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long readLong() {
        byte[] b = buffer_;
        int pos = position_;
        if (pos + CodecUtil.LONG_BYTES > end_) {
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
        position_ = pos;
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

    @Override
    public String readString(CharsetDecoder charsetDecoder, int bytes) {
        String cached = StringCache.getCachedValue(this, charsetDecoder, bytes);
        if (cached != null) {
            return cached;
        }

        float charsPerByte = charsetDecoder.averageCharsPerByte();
        ByteBuffer input = ByteBuffer.wrap(buffer_, position_, bytes);
        CharBuffer output = CharBuffer.allocate(Buffers.outputCharBufferSize(charsPerByte, bytes));
        for (;;) {
            CoderResult cr = charsetDecoder.decode(input, output, true);
            if (!cr.isError() && !cr.isOverflow()) {
                position_ = input.position();
                break;
            }
            if (cr.isOverflow()) {
                output = Buffers.expand(output, charsPerByte, input.remaining());
                continue;
            }
            if (cr.isError()) {
                position_ = input.position();
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
        int pos = position_;
        int n = end_ - pos; // remaining
        if (bytes < n) {
            n = (bytes < -pos) ? -pos : bytes;
        }
        position_ += n;
        return n;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int remainingBytes() {
        return end_ - position_;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int limitBytes() {
        return end_;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clear() {
        position_ = 0;
        offset_ = 0;
        end_ = 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BufferSink toBufferSink() {
        return new ArrayBufferSink(buffer_, offset_, end_);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ByteBuffer toByteBuffer() {
        return ByteBuffer.wrap(buffer_, offset_, end_);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasArray() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public byte[] toArray() {
        return buffer_;
    }

    /**
     * {@inheritDoc}
     */
    public int arrayOffset() {
        return offset_;
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
        int capacity = buffer_.length;
        int space = capacity - end_;
        if (space < bytes) {
            int required = end_ + bytes;
            int twice = capacity * 2;
            buffer_ = Arrays.copyOf(buffer_, (required >= twice) ? required : twice);
        }
        input.readBytes(buffer_, end_, bytes);
        end_ += bytes;
        return bytes;
    }
}
