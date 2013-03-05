package net.ihiroky.niotty.buffer;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.util.Arrays;

/**
 * Implementation of {@link net.ihiroky.niotty.buffer.CodecBuffer} using {@code byte[]}.
 *
 * @author Hiroki Itoh
 */
public class ArrayCodecBuffer extends AbstractCodecBuffer implements CodecBuffer {

    private byte[] buffer_;
    private int offset_;
    private int beginning_;
    private int end_;

    private static final int EXPAND_MULTIPLIER = 2;
    private static final int DEFAULT_CAPACITY = 512;

    ArrayCodecBuffer() {
        this(DEFAULT_CAPACITY);
    }

    ArrayCodecBuffer(int initialCapacity) {
        if (initialCapacity < 0) {
            throw new IllegalArgumentException("initialCapacity must not be negative.");
        }
        buffer_ = new byte[initialCapacity];
    }

    ArrayCodecBuffer(byte[] b, int offset, int length) {
        if (offset + length > b.length) {
            throw new IndexOutOfBoundsException(
                    "offset + length (" + (offset + length) + ") exceeds buffer capacity " + b.length);
        }
        buffer_ = b;
        this.offset_ = offset;
        beginning_ = offset;
        end_ = offset + length;
    }

    /**
     * Ensures the backed byte array capacity. The new capacity is the large of the two, sum of the current position
     * and {@code length}, and twice the size of current capacity.
     *
     * @param space the size of byte to be written
     */
    void ensureSpace(int space) {
        int capacity = buffer_.length;
        int required = end_ + space;
        if (required >= capacity) {
            int twice = capacity * EXPAND_MULTIPLIER;
            buffer_ = Arrays.copyOf(buffer_, (required >= twice) ? required : twice);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeByte(int value) {
        ensureSpace(1);
        buffer_[end_++] = (byte) value;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeBytes(byte[] bytes, int offset, int length) {
        ensureSpace(length);
        System.arraycopy(bytes, offset, buffer_, end_, length);
        end_ += length;
    }

    /**
     * {@inheritDoc}
     */
    public void writeBytes(ByteBuffer byteBuffer) {
        int remaining = byteBuffer.remaining();
        ensureSpace(remaining);
        byteBuffer.get(buffer_, end_, remaining);
        end_ += remaining;
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
     * Implementation of writeBytes8() without {@code byte} range check.
     * @param bits the set of bit to be written
     * @param bytes the byte size of {@code bits}
     */
    private void writeBytes8RangeUnchecked(long bits, int bytes) {
        ensureSpace(bytes);
        int bytesMinus1 = bytes - 1;
        int base = end_;
        byte[] b = buffer_;
        for (int i = 0; i < bytes; i++) {
            b[base + i] = (byte) ((bits >>> (bytesMinus1 - i)) & CodecUtil.BYTE_MASK);
        }
        end_ = base + bytes;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeShort(short value) {
        ensureSpace(CodecUtil.SHORT_BYTES);
        int c = end_;
        buffer_[c] = (byte) ((value >>> CodecUtil.BYTE_SHIFT1) & CodecUtil.BYTE_MASK);
        buffer_[c + 1] = (byte) (value & CodecUtil.BYTE_MASK);
        end_ = c + CodecUtil.SHORT_BYTES;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeChar(char value) {
        ensureSpace(CodecUtil.CHAR_BYTES);
        int c = end_;
        buffer_[c] = (byte) ((value >>> CodecUtil.BYTE_SHIFT1) & CodecUtil.BYTE_MASK);
        buffer_[c + 1] = (byte) (value & CodecUtil.BYTE_MASK);
        end_ = c + CodecUtil.CHAR_BYTES;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeInt(int value) {
        ensureSpace(CodecUtil.INT_BYTES);
        int c = end_;
        byte[] b = buffer_;
        int offset = 1;
        b[c] = (byte) ((value >>> CodecUtil.BYTE_SHIFT3) & CodecUtil.BYTE_MASK);
        b[c + offset++] = (byte) ((value >>> CodecUtil.BYTE_SHIFT2) & CodecUtil.BYTE_MASK);
        b[c + offset++] = (byte) ((value >>> CodecUtil.BYTE_SHIFT1) & CodecUtil.BYTE_MASK);
        b[c + offset] = (byte) (value & CodecUtil.BYTE_MASK);
        end_ = c + CodecUtil.INT_BYTES;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeLong(long value) {
        ensureSpace(CodecUtil.LONG_BYTES);
        int c = end_;
        byte[] b = buffer_;
        int offset = 1;
        b[c] = (byte) ((value >>> CodecUtil.BYTE_SHIFT7) & CodecUtil.BYTE_MASK);
        b[c + offset++] = (byte) ((value >>> CodecUtil.BYTE_SHIFT6) & CodecUtil.BYTE_MASK);
        b[c + offset++] = (byte) ((value >>> CodecUtil.BYTE_SHIFT5) & CodecUtil.BYTE_MASK);
        b[c + offset++] = (byte) ((value >>> CodecUtil.BYTE_SHIFT4) & CodecUtil.BYTE_MASK);
        b[c + offset++] = (byte) ((value >>> CodecUtil.BYTE_SHIFT3) & CodecUtil.BYTE_MASK);
        b[c + offset++] = (byte) ((value >>> CodecUtil.BYTE_SHIFT2) & CodecUtil.BYTE_MASK);
        b[c + offset++] = (byte) ((value >>> CodecUtil.BYTE_SHIFT1) & CodecUtil.BYTE_MASK);
        b[c + offset] = (byte) (value & CodecUtil.BYTE_MASK);
        end_ = c + CodecUtil.LONG_BYTES;
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

        if (StringCache.writeEmptyOrOneCharAscii(this, charsetEncoder, s)) {
            return;
        }

        int length = s.length();
        int expectedLengthBytes = CodecUtil.variableByteLength(
                Buffers.outputByteBufferSize(charsetEncoder.averageBytesPerChar(), length));
        ensureSpace(expectedLengthBytes);
        end_ += expectedLengthBytes;
        int startPosition = end_;
        CharBuffer input = CharBuffer.wrap(s);
        ByteBuffer output = ByteBuffer.wrap(buffer_, end_, buffer_.length - end_);
        for (;;) {
            CoderResult cr = charsetEncoder.encode(input, output, true);
            if (!cr.isError() && !cr.isOverflow()) {
                end_ = output.position();
                break;
            }
            if (cr.isOverflow()) {
                end_ = output.position();
                ensureSpace((int) (charsetEncoder.averageBytesPerChar() * input.remaining() + 1));
                output = ByteBuffer.wrap(buffer_, end_, buffer_.length - end_);
                continue;
            }
            if (cr.isError()) {
                end_ = output.position();
                try {
                    cr.throwException();
                } catch (CharacterCodingException cce) {
                    throw new RuntimeException(cce);
                }
            }
        }
        int outputLength = end_ - startPosition;
        int lengthBytes = CodecUtil.variableByteLength(outputLength);
        if (lengthBytes != expectedLengthBytes) {
            int delta = lengthBytes - expectedLengthBytes;
            ensureSpace(delta);
            System.arraycopy(buffer_, startPosition, buffer_, startPosition + delta, outputLength);
            end_ += delta;
        }
        int tmp = end_;
        end_ = startPosition - expectedLengthBytes;
        writeVariableByteInteger(outputLength);
        end_ = tmp;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int readByte() {
        if (beginning_ >= end_) {
            throw new IndexOutOfBoundsException("position exceeds end of buffer.");
        }
        return buffer_[beginning_++] & CodecUtil.BYTE_MASK;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readBytes(byte[] bytes, int offset, int length) {
        int beginning = beginning_;
        if (beginning + length > end_) {
            throw new IndexOutOfBoundsException("beginning exceeds end if " + length + " byte is read.");
        }
        System.arraycopy(buffer_, beginning, bytes, offset, length);
        beginning_ += length;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readBytes(ByteBuffer byteBuffer) {
        int space = byteBuffer.remaining();
        int remaining = remainingBytes();
        int read = (space <= remaining) ? space : remaining;
        byteBuffer.put(buffer_, beginning_, read);
        beginning_ += read;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int readBytes4(int bytes) {
        if (bytes < 0 || bytes > CodecUtil.INT_BYTES) {
            throw new IllegalArgumentException("bytes must be in [0, 4].");
        }

        int pos = beginning_;
        if (pos + bytes > end_) {
            throw new IndexOutOfBoundsException("beginning exceeds end if " + bytes + " byte is read.");
        }

        byte[] b = buffer_;
        int decoded = 0;
        for (int i = 0; i < bytes; i++) {
            decoded = (decoded << CodecUtil.BITS_PER_BYTE) | (b[pos++] & CodecUtil.BYTE_MASK);
        }
        beginning_ = pos;
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

        int pos = beginning_;
        if (pos + bytes > end_) {
            throw new IndexOutOfBoundsException("beginning exceeds end if " + bytes + " byte is read.");
        }

        byte[] b = buffer_;
        long decoded = 0L;
        for (int i = 0; i < bytes; i++) {
            decoded = (decoded << CodecUtil.BITS_PER_BYTE) | (b[pos++] & CodecUtil.BYTE_MASK);
        }
        beginning_ = pos;
        return decoded;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public char readChar() {
        if (beginning_ + CodecUtil.CHAR_BYTES > end_) {
            throw new IndexOutOfBoundsException("position exceeds the end of buffer if read char.");
        }
        return (char) (((buffer_[beginning_++] & CodecUtil.BYTE_MASK) << CodecUtil.BYTE_SHIFT1)
                | (buffer_[beginning_++] & CodecUtil.BYTE_MASK));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public short readShort() {
        if (beginning_ + CodecUtil.SHORT_BYTES > end_) {
            throw new IndexOutOfBoundsException("position exceeds the end of buffer if read short.");
        }
        return (short) (((buffer_[beginning_++] & CodecUtil.BYTE_MASK) << CodecUtil.BYTE_SHIFT1)
                | (buffer_[beginning_++] & CodecUtil.BYTE_MASK));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int readInt() {
        byte[] b = buffer_;
        int pos = beginning_;
        if (pos + CodecUtil.INT_BYTES > end_) {
            throw new IndexOutOfBoundsException("position exceeds the end of buffer if read int wide byte.");
        }
        int result = (((b[pos++] & CodecUtil.BYTE_MASK) << CodecUtil.BYTE_SHIFT3)
                | ((b[pos++] & CodecUtil.BYTE_MASK) << CodecUtil.BYTE_SHIFT2)
                | ((b[pos++] & CodecUtil.BYTE_MASK) << CodecUtil.BYTE_SHIFT1)
                |  (b[pos++] & CodecUtil.BYTE_MASK));
        beginning_ = pos;
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long readLong() {
        byte[] b = buffer_;
        int pos = beginning_;
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
        beginning_ = pos;
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
        ByteBuffer input = ByteBuffer.wrap(buffer_, beginning_, bytes);
        CharBuffer output = CharBuffer.allocate(Buffers.outputCharBufferSize(charsPerByte, bytes));
        for (;;) {
            CoderResult cr = charsetDecoder.decode(input, output, true);
            if (!cr.isError() && !cr.isOverflow()) {
                beginning_ = input.position();
                break;
            }
            if (cr.isOverflow()) {
                output = Buffers.expand(output, charsPerByte, input.remaining());
                continue;
            }
            if (cr.isError()) {
                beginning_ = input.position();
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
        int pos = beginning_;
        int n = end_ - pos; // remaining
        if (bytes < n) {
            n = (bytes < -pos) ? -pos : bytes;
        }
        beginning_ += n;
        return n;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean transferTo(WritableByteChannel channel, ByteBuffer writeBuffer) throws IOException {
        int remaining = end_ - beginning_;
        while (remaining > 0) {
            int space = writeBuffer.remaining();
            int readyToWrite = (remaining <= space) ? remaining : space;
            writeBuffer.put(buffer_, beginning_, readyToWrite);
            writeBuffer.flip();
            int writeBytes = channel.write(writeBuffer);
            // Write operation is failed.
            if (writeBytes == -1) {
                throw new EOFException();
            }
            remaining -= writeBytes;
            // Some bytes remains in writeBuffer. Stop this round
            if (writeBytes < readyToWrite) {
                beginning_ += readyToWrite;
                return false;
            }
            writeBuffer.clear();
        }
        beginning_ = end_;
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int remainingBytes() {
        return end_ - beginning_;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int priority() {
        return -1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int spaceBytes() {
        return buffer_.length - end_;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int capacityBytes() {
        return buffer_.length;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int beginning() {
        return beginning_;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CodecBuffer beginning(int beginning) {
        if (beginning < 0) {
            throw new IndexOutOfBoundsException("beginning is negative.");
        }
        if (beginning > end_) {
            throw new IndexOutOfBoundsException("beginning is greater than end.");
        }
        beginning_ = beginning;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int end() {
        return end_;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CodecBuffer end(int end) {
        if (end < beginning_) {
            throw new IndexOutOfBoundsException("end is less than beginning.");
        }
        if (end > buffer_.length) {
            throw new IndexOutOfBoundsException("end is greater than capacity.");
        }
        end_ = end;
        return this;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public CodecBuffer clear() {
        beginning_ = 0;
        offset_ = 0;
        end_ = 0;
        return this;
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
    public int drainFrom(CodecBuffer buffer) {
        return drainFromNoCheck(buffer, buffer.remainingBytes());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int drainFrom(CodecBuffer buffer, int bytes) {
        if (bytes < 0) {
            throw new IllegalArgumentException("bytes must be >= 0.");
        }
        int remaining = buffer.remainingBytes();
        return drainFromNoCheck(buffer, (bytes <= remaining) ? bytes : remaining);
    }

    /**
     * Drains data from {@code input} with specified bytes.
     * @param input buffer which contains the data transferred from
     * @param bytes the number of byte to be transferred
     * @return {@code bytes}
     */
    private int drainFromNoCheck(CodecBuffer input, int bytes) {
        ensureSpace(bytes);
        input.readBytes(buffer_, end_, bytes);
        end_ += bytes;
        return bytes;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CodecBuffer slice(int bytes) {
        if (bytes < 0 || bytes > remainingBytes()) {
            throw new IllegalArgumentException("Invalid input " + bytes + ". " + remainingBytes() + " byte remains.");
        }
        int position = beginning_;
        beginning_ += bytes;
        return new ArrayCodecBuffer(buffer_, position, bytes);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CodecBuffer compact() {
        int remaining = remainingBytes();
        System.arraycopy(buffer_, beginning_, buffer_, 0, remaining);
        beginning_ = 0;
        end_ = remaining;
        offset_ = 0;
        return this;
    }

    /**
     * Returns a summary of this buffer state.
     * @return a summary of this buffer state
     */
    @Override
    public String toString() {
        return "(beginning:" + beginning_ + ", end:" + end_ + ", capacity:" + buffer_.length + ')';
    }
}
