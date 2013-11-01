package net.ihiroky.niotty.buffer;

import net.ihiroky.niotty.util.Arguments;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.GatheringByteChannel;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;

/**
 * A {@link CodecBuffer} whose content is a shared subsequence of the specified
 * {@code base}'s content.
 * <p></p>
 * The content of this object will start at this {@code base}'s current startIndex. Changes to the content
 * of the {@code base} will be visible in the object, and vice versa; the two object's startIndex and endIndex
 * will be independent.
 * <p></p>
 * The new {@code SlicedCodecBuffer}'s startIndex will be zero, its endIndex and capacity will be the number of bytes
 * remaining in the {@code base} buffer. The new one is not expandable.
 * @author Hiroki Itoh
 */
public class SlicedCodecBuffer extends AbstractCodecBuffer {

    /** the base {@code CodecBuffer}. */
    final CodecBuffer base_;

    /** a offset value for the base's startIndex. */
    final int offset_;

    final int capacity_; // fix capacity to avoid region over wrap.

    /**
     * Creates a new instance.
     *
     * @param base the base {@code CodecBuffer}.
     */
    SlicedCodecBuffer(CodecBuffer base) {
        base_ = base;
        offset_ = base.startIndex();
        capacity_ = base.endIndex();
    }

    /**
     * Creates a new instance. The endIndex of the {@code base} changes to {@code base.startIndex() + bytes}.
     *
     * @param base the base {@code CodecBuffer}.
     * @param bytes the data size by the bytes to be sliced.
     * @throws IllegalArgumentException if the {@code bytes} is greater the remaining of the {@code base}.
     */
    SlicedCodecBuffer(CodecBuffer base, int bytes) {
        int beginning = base.startIndex();
        int capacity = beginning + bytes;
        if (capacity > base.endIndex()) {
            throw new IllegalArgumentException("capacity must be less than or equal base.endIndex().");
        }

        base.endIndex(capacity);
        base_ = base;
        offset_ = beginning;
        capacity_ = capacity;
    }

    private SlicedCodecBuffer(CodecBuffer base, int offset, int capacity) {
        base_ = base.duplicate();
        offset_ = offset;
        capacity_ = capacity;
    }

    void checkSpace(int bytes) {
        if (base_.endIndex() + bytes > capacity_) {
            throw new IndexOutOfBoundsException("no space is left. required: " + bytes + ", space: " + space());
        }
    }

    @Override
    public SlicedCodecBuffer writeByte(int value) {
        checkSpace(1);
        base_.writeByte(value);
        return this;
    }

    @Override
    public SlicedCodecBuffer writeBytes(byte[] bytes, int offset, int length) {
        checkSpace(length);
        base_.writeBytes(bytes, offset, length);
        return this;
    }

    @Override
    public SlicedCodecBuffer writeBytes(ByteBuffer byteBuffer) {
        checkSpace(byteBuffer.remaining());
        base_.writeBytes(byteBuffer);
        return this;
    }

    @Override
    public SlicedCodecBuffer writeShort(int value) {
        checkSpace(CodecUtil.SHORT_BYTES);
        base_.writeShort(value);
        return this;
    }

    @Override
    public SlicedCodecBuffer writeChar(char value) {
        checkSpace(CodecUtil.CHAR_BYTES);
        base_.writeChar(value);
        return this;
    }

    @Override
    public SlicedCodecBuffer writeMedium(int value) {
        checkSpace(CodecUtil.MEDIUM_BYTES);
        base_.writeMedium(value);
        return this;
    }

    @Override
    public SlicedCodecBuffer writeInt(int value) {
        checkSpace(CodecUtil.INT_BYTES);
        base_.writeInt(value);
        return this;
    }

    @Override
    public SlicedCodecBuffer writeLong(long value) {
        checkSpace(CodecUtil.LONG_BYTES);
        base_.writeLong(value);
        return this;
    }

    @Override
    public SlicedCodecBuffer writeString(String s, CharsetEncoder encoder) {
        Arguments.requireNonNull(s, "s");
        Arguments.requireNonNull(encoder, "encoder");

        CharBuffer input = CharBuffer.wrap(s);
        ByteBuffer output = base_.byteBuffer();
        output.position(output.limit());
        output.limit(output.capacity());
        for (;;) {
            CoderResult cr = encoder.encode(input, output, true);
            if (cr.isUnderflow()) {
                cr = encoder.flush(output);
                if (cr.isUnderflow()) {
                    base_.endIndex(output.position());
                    encoder.reset();
                    break;
                }
                // jump ot overflow operation.
            }
            if (cr.isOverflow()) {
                throw new IndexOutOfBoundsException("no space left to write [" + s + "].");
            }
            if (cr.isError()) {
                try {
                    cr.throwException();
                } catch (CharacterCodingException cce) {
                    throw new RuntimeException(cce);
                }
            }
        }
        return this;
    }

    @Override
    public byte readByte() {
        return base_.readByte();
    }

    @Override
    public int readUnsignedByte() {
        return base_.readUnsignedByte();
    }

    @Override
    public int readBytes(byte[] bytes, int offset, int length) {
        return base_.readBytes(bytes, offset, length);
    }

    @Override
    public int readBytes(ByteBuffer byteBuffer) {
        return base_.readBytes(byteBuffer);
    }

    @Override
    public char readChar() {
        return base_.readChar();
    }

    @Override
    public short readShort() {
        return base_.readShort();
    }

    @Override
    public int readUnsignedShort() {
        return base_.readUnsignedShort();
    }

    @Override
    public int readUnsignedMedium() {
        return base_.readUnsignedMedium();
    }

    @Override
    public int readInt() {
        return base_.readInt();
    }

    @Override
    public long readUnsignedInt() {
        return base_.readUnsignedInt();
    }

    @Override
    public long readLong() {
        return base_.readLong();
    }

    @Override
    public String readString(CharsetDecoder decoder, int bytes) {
        return base_.readString(decoder, bytes);
    }

    @Override
    public int skipStartIndex(int bytes) {
        int pos = base_.startIndex();
        int n = base_.endIndex() - pos; // remaining
        if (bytes < n) {
            n = (bytes < -(pos - offset_)) ? -(pos - offset_) : bytes;
        }
        return base_.skipStartIndex(n);
    }

    @Override
    public int skipEndIndex(int n) {
        int pos = base_.endIndex();
        int s = capacity_ - pos; // space
        if (n < s) {
            int r = pos - base_.startIndex(); // remaining
            s = (n < -r) ? -r : n;
        }
        return base_.skipEndIndex(s);
    }

    @Override
    public int remaining() {
        return base_.remaining();
    }

    @Override
    public void dispose() {
        base_.dispose();
    }

    @Override
    public int space() {
        return capacity_ - base_.endIndex();
    }

    @Override
    public int capacity() {
        return capacity_ - offset_;
    }

    @Override
    public int startIndex() {
        return base_.startIndex() - offset_;
    }

    @Override
    public CodecBuffer startIndex(int beginning) {
        base_.startIndex(beginning + offset_);
        return this;
    }

    @Override
    public int endIndex() {
        return base_.endIndex() - offset_;
    }

    @Override
    public CodecBuffer endIndex(int end) {
        base_.endIndex(end + offset_);
        return this;
    }

    @Override
    public int drainFrom(CodecBuffer buffer) {
        checkSpace(buffer.remaining());
        return base_.drainFrom(buffer);
    }

    @Override
    public int drainFrom(CodecBuffer buffer, int bytes) {
        checkSpace(bytes);
        return base_.drainFrom(buffer, bytes);
    }

    @Override
    public boolean transferTo(GatheringByteChannel channel) throws IOException {
        return base_.transferTo(channel);
    }

    @Override
    public void copyTo(ByteBuffer buffer) {
        base_.copyTo(buffer);
    }

    @Override
    public BufferSink addFirst(CodecBuffer buffer) {
        int required = buffer.remaining();
        int frontSpace = base_.startIndex() - offset_; // ignore space at tail.
        if (required > frontSpace) {
            throw new IndexOutOfBoundsException(
                    "no space is left. required: " + required + ", front space: " + frontSpace);
        }
        base_.addFirst(buffer);
        return this;
    }

    @Override
    public BufferSink addLast(CodecBuffer buffer) {
        int required = buffer.remaining();
        int backSpace = capacity_ - base_.endIndex(); // ignore space at head.
        if (required > backSpace) {
            throw new IndexOutOfBoundsException(
                    "no space is left. required: " + required + ", back space: " + backSpace);
        }
        base_.addLast(buffer);
        return this;
    }

    @Override
    public CodecBuffer slice(int bytes) {
        return base_.slice(bytes);
    }

    @Override
    public CodecBuffer slice() {
        return new SlicedCodecBuffer(this);
    }

    @Override
    public CodecBuffer duplicate() {
        return new SlicedCodecBuffer(base_, offset_, capacity_);
    }

    @Override
    public CodecBuffer compact() {
        int frontSpace = base_.startIndex() - offset_;
        if (frontSpace == 0) {
            return this;
        }

        if (base_.hasArray()) {
            byte[] b = base_.array();
            int os = base_.arrayOffset() + offset_;
            System.arraycopy(b, os + frontSpace, b, os, base_.remaining());
            base_.startIndex(offset_);
            base_.endIndex(base_.endIndex() - frontSpace);
        } else {
            ByteBuffer bb = base_.byteBuffer();
            bb.position(offset_).limit(capacity_);
            ByteBuffer s = bb.slice();
            s.position(frontSpace);
            s.compact();
            base_.startIndex(offset_).endIndex(base_.endIndex() - frontSpace);
        }
        return this;
    }

    @Override
    public CodecBuffer clear() {
        base_.startIndex(offset_);
        base_.endIndex(offset_);
        return this;
    }

    @Override
    public ByteBuffer byteBuffer() {
        ByteBuffer bb = base_.byteBuffer();
        int position = bb.position();
        bb.position(offset_);
        ByteBuffer sliced = bb.slice();
        sliced.position(position - offset_);
        return sliced;
    }

    @Override
    public boolean hasArray() {
        return base_.hasArray();
    }

    @Override
    public byte[] array() {
        return base_.array();
    }

    @Override
    public int arrayOffset() {
        return base_.arrayOffset() + offset_;
    }

    @Override
    public int indexOf(int b, int fromIndex) {
        return base_.indexOf(b, fromIndex + offset_);
    }

    @Override
    public int indexOf(byte[] b, int fromIndex) {
        return base_.indexOf(b, fromIndex + offset_);
    }

    @Override
    public int lastIndexOf(int b, int fromIndex) {
        return base_.lastIndexOf(b, fromIndex + offset_);
    }

    @Override
    public int lastIndexOf(byte[] b, int fromIndex) {
        return base_.lastIndexOf(b, fromIndex + offset_);
    }

    /**
     * Returns a summary of this buffer state.
     * @return a summary of this buffer state
     */
    @Override
    public String toString() {
        return SlicedCodecBuffer.class.getName()
                + "(startIndex:" + startIndex() + ", endIndex:" + endIndex() + ", capacity:" + capacity()
                + ", offset:" + offset_ + ')';
    }
}
