package net.ihiroky.niotty.buffer;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.util.Arrays;

/**
 * Implementation of {@link net.ihiroky.niotty.buffer.EncodeBuffer} using {@code byte[]}.
 *
 * @author Hiroki Itoh
 */
public class ArrayEncodeBuffer extends AbstractEncodeBuffer implements EncodeBuffer {

    private byte[] buffer_;
    private int offset_;
    private int position_;

    private static final int DEFAULT_CAPACITY = 512;
    private static final int MINIMUM_CAPACITY = 8;

    ArrayEncodeBuffer() {
        this(DEFAULT_CAPACITY);
    }

    ArrayEncodeBuffer(int initialCapacity) {
        int capacity = initialCapacity;
        if (capacity <= MINIMUM_CAPACITY) {
            capacity = MINIMUM_CAPACITY;
        }
        buffer_ = new byte[capacity];
    }

    ArrayEncodeBuffer(byte[] buffer, int offset, int length) {
        if (offset + length > buffer.length) {
            throw new IndexOutOfBoundsException(
                    "offset + length (" + (offset + length) + ") exceeds buffer capacity " + buffer.length);
        }
        this.buffer_ = buffer;
        this.offset_ = offset;
        this.position_ = offset + length;

    }

    /**
     * Ensures the backed byte array capacity. The new capacity is the large of the two, sum of the current position
     * and {@code length}, and twice the size of current capacity.
     *
     * @param length the size of byte to be written
     */
    void ensureSpace(int length) {
        int current = buffer_.length;
        int required = position_ + length;
        if (required >= current) {
            int twice = current * 2;
            int newCapacity = (required >= twice) ? required : twice;
            buffer_ = Arrays.copyOf(buffer_, newCapacity);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeByte(int value) {
        ensureSpace(1);
        buffer_[position_++] = (byte) value;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeBytes(byte[] bytes, int offset, int length) {
        ensureSpace(length);
        System.arraycopy(bytes, offset, buffer_, position_, length);
        position_ += length;
    }

    /**
     * {@inheritDoc}
     */
    public void writeBytes(ByteBuffer byteBuffer) {
        int remaining = byteBuffer.remaining();
        ensureSpace(remaining);
        byteBuffer.get(buffer_, position_, remaining);
        position_ += remaining;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeBytes4(int bits, int bytes) {
        if (bytes < 0 || bytes > CodecUtil.INT_BYTES) {
            throw new IllegalArgumentException("bytes must be in int bytes.");
        }
        writeBytes8RangeUnchecked(bits, bytes);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeBytes8(long bits, int bytes) {
        if (bytes < 0 || bytes > CodecUtil.LONG_BYTES) {
            throw new IllegalArgumentException("bytes must be in long bytes.");
        }
        writeBytes8RangeUnchecked(bits, bytes);
    }

    /**
     * Implementation of wirteBytes8() without {@code byte} range check.
     * @param bits the set of bit to be written
     * @param bytes the byte size of {@code bits}
     */
    private void writeBytes8RangeUnchecked(long bits, int bytes) {
        ensureSpace(bytes);
        int bytesMinus1 = bytes - 1;
        int base = position_;
        byte[] b = buffer_;
        for (int i = 0; i < bytes; i++) {
            b[base + i] = (byte) ((bits >>> (bytesMinus1 - i)) & CodecUtil.BYTE_MASK);
        }
        position_ = base + bytes;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeShort(short value) {
        ensureSpace(CodecUtil.SHORT_BYTES);
        int c = position_;
        buffer_[c] = (byte) ((value >>> CodecUtil.BYTE_SHIFT1) & CodecUtil.BYTE_MASK);
        buffer_[c + 1] = (byte) (value & CodecUtil.BYTE_MASK);
        position_ = c + CodecUtil.SHORT_BYTES;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeChar(char value) {
        ensureSpace(CodecUtil.CHAR_BYTES);
        int c = position_;
        buffer_[c] = (byte) ((value >>> CodecUtil.BYTE_SHIFT1) & CodecUtil.BYTE_MASK);
        buffer_[c + 1] = (byte) (value & CodecUtil.BYTE_MASK);
        position_ = c + CodecUtil.CHAR_BYTES;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeInt(int value) {
        ensureSpace(CodecUtil.INT_BYTES);
        int c = position_;
        byte[] b = buffer_;
        b[c] = (byte) ((value >>> CodecUtil.BYTE_SHIFT3) & CodecUtil.BYTE_MASK);
        b[c + 1] = (byte) ((value >>> CodecUtil.BYTE_SHIFT2) & CodecUtil.BYTE_MASK);
        b[c + 2] = (byte) ((value >>> CodecUtil.BYTE_SHIFT1) & CodecUtil.BYTE_MASK);
        b[c + 3] = (byte) (value & CodecUtil.BYTE_MASK);
        position_ = c + CodecUtil.INT_BYTES;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeLong(long value) {
        ensureSpace(CodecUtil.LONG_BYTES);
        int c = position_;
        byte[] b = buffer_;
        b[c] = (byte) ((value >>> CodecUtil.BYTE_SHIFT7) & CodecUtil.BYTE_MASK);
        b[c + 1] = (byte) ((value >>> CodecUtil.BYTE_SHIFT6) & CodecUtil.BYTE_MASK);
        b[c + 2] = (byte) ((value >>> CodecUtil.BYTE_SHIFT5) & CodecUtil.BYTE_MASK);
        b[c + 3] = (byte) ((value >>> CodecUtil.BYTE_SHIFT4) & CodecUtil.BYTE_MASK);
        b[c + 4] = (byte) ((value >>> CodecUtil.BYTE_SHIFT3) & CodecUtil.BYTE_MASK);
        b[c + 5] = (byte) ((value >>> CodecUtil.BYTE_SHIFT2) & CodecUtil.BYTE_MASK);
        b[c + 6] = (byte) ((value >>> CodecUtil.BYTE_SHIFT1) & CodecUtil.BYTE_MASK);
        b[c + 7] = (byte) (value & CodecUtil.BYTE_MASK);
        position_ = c + CodecUtil.LONG_BYTES;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeFloat(float value) {
        writeInt(Float.floatToIntBits(value));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeDouble(double value) {
        writeLong(Double.doubleToLongBits(value));
    }

    /**
     * {@inheritDoc}
     */
    public void writeString(CharsetEncoder charsetEncoder, String s) {
        if (s == null) {
            throw new NullPointerException("s must not be null.");
        }
        int length = s.length();
        if (length == 0) {
            return;
        }
        if (length == 1 && StringCache.writeAsOneCharAscii(this, charsetEncoder, s)) {
            return;
        }
        int expectedLengthBytes = CodecUtil.variableByteLength(
                Buffers.outputByteBufferSize(charsetEncoder.averageBytesPerChar(), length));
        ensureSpace(expectedLengthBytes);
        position_ += expectedLengthBytes;
        int startPosition = position_;
        CharBuffer input = CharBuffer.wrap(s);
        ByteBuffer output = ByteBuffer.wrap(buffer_, position_, buffer_.length - position_);
        for (;;) {
            CoderResult cr = charsetEncoder.encode(input, output, true);
            if (!cr.isError() && !cr.isOverflow()) {
                position_ = output.position();
                break;
            }
            if (cr.isOverflow()) {
                position_ = output.position();
                ensureSpace((int) (charsetEncoder.averageBytesPerChar() * input.remaining() + 1));
                output = ByteBuffer.wrap(buffer_, position_, buffer_.length - position_);
                continue;
            }
            if (cr.isError()) {
                position_ = output.position();
                try {
                    cr.throwException();
                } catch (CharacterCodingException cce) {
                    throw new RuntimeException(cce);
                }
            }
        }
        int outputLength = position_ - startPosition;
        int lengthBytes = CodecUtil.variableByteLength(outputLength);
        if (lengthBytes != expectedLengthBytes) {
            int delta = lengthBytes - expectedLengthBytes;
            ensureSpace(delta);
            System.arraycopy(buffer_, startPosition, buffer_, startPosition + delta, outputLength);
            position_ += delta;
        }
        int tmp = position_;
        position_ = startPosition - expectedLengthBytes;
        writeVariableByteInteger(outputLength);
        position_ = tmp;
    }

    @Override
    public void drainTo(EncodeBuffer encodeBuffer) {
        encodeBuffer.writeBytes(buffer_, offset_, position_);
        position_ = offset_;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int filledBytes() {
        return position_ - offset_;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int limitBytes() {
        return buffer_.length;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clear() {
        position_ = 0;
        offset_ = 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BufferSink createBufferSink() {
        return new ArrayBufferSink(buffer_, offset_, position_);
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasArray() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
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
    public ByteBuffer toByteBuffer() {
        return ByteBuffer.wrap(buffer_, offset_, position_);
    }
}
