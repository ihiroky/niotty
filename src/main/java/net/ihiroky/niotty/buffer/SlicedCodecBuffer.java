package net.ihiroky.niotty.buffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.GatheringByteChannel;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.util.Objects;

/**
 * A {@link net.ihiroky.niotty.buffer.CodecBuffer} whose content is a shared subsequence of the specified
 * {@code base}'s content.
 * <p></p>
 * The content of this object will start at this {@code base}'s current beginning. Changes to the content
 * of the {@code base} will be visible in the object, and vice versa; the two object's beginning and end
 * will be independent.
 * <p></p>
 * The new {@code SlicedCodecBuffer}'s beginning will be zero, its end and capacity will be the number of bytes
 * remaining in the {@code base} buffer. The new one is not expandable.
 * @author Hiroki Itoh
 */
public class SlicedCodecBuffer extends AbstractCodecBuffer {

    /** the base {@code CodecBuffer}. */
    final CodecBuffer base_;

    /** a offset value for the base's beginning. */
    final int offset_;

    final int capacity_; // fix capacity to avoid region over wrap.

    /**
     * Creates a new instance.
     *
     * @param base the base {@code CodecBuffer}.
     */
    SlicedCodecBuffer(CodecBuffer base) {
        base_ = base;
        offset_ = base.beginning();
        capacity_ = base.end();
    }

    /**
     * Creates a new instance. The end of the {@code base} changes to {@code base.beginning() + bytes}.
     *
     * @param base the base {@code CodecBuffer}.
     * @param bytes the data size by the bytes to be sliced.
     * @throws IllegalArgumentException if the {@code bytes} is greater the remaining of the {@code base}.
     */
    SlicedCodecBuffer(CodecBuffer base, int bytes) {
        int beginning = base.beginning();
        int capacity = beginning + bytes;
        if (capacity > base.end()) {
            throw new IllegalArgumentException("capacity must be less than or equal base.end().");
        }

        base.end(capacity);
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
        if (base_.end() + bytes > capacity_) {
            throw new IndexOutOfBoundsException("no space is left. required: " + bytes + ", space: " + spaceBytes());
        }
    }

    @Override
    public void writeByte(int value) {
        checkSpace(1);
        base_.writeByte(value);
    }

    @Override
    public void writeBytes(byte[] bytes, int offset, int length) {
        checkSpace(length);
        base_.writeBytes(bytes, offset, length);
    }

    @Override
    public void writeBytes(ByteBuffer byteBuffer) {
        checkSpace(byteBuffer.remaining());
        base_.writeBytes(byteBuffer);
    }

    @Override
    public void writeShort(short value) {
        checkSpace(CodecUtil.SHORT_BYTES);
        base_.writeShort(value);
    }

    @Override
    public void writeChar(char value) {
        checkSpace(CodecUtil.CHAR_BYTES);
        base_.writeChar(value);
    }

    @Override
    public void writeInt(int value) {
        checkSpace(CodecUtil.INT_BYTES);
        base_.writeInt(value);
    }

    @Override
    public void writeLong(long value) {
        checkSpace(CodecUtil.LONG_BYTES);
        base_.writeLong(value);
    }

    @Override
    public void writeString(String s, CharsetEncoder encoder) {
        Objects.requireNonNull(s, "s");
        Objects.requireNonNull(encoder, "encoder");

        CharBuffer input = CharBuffer.wrap(s);
        ByteBuffer output = base_.toByteBuffer();
        output.position(output.limit());
        output.limit(output.capacity());
        for (;;) {
            CoderResult cr = encoder.encode(input, output, true);
            if (cr.isUnderflow()) {
                cr = encoder.flush(output);
                if (cr.isUnderflow()) {
                    base_.end(output.position());
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
    }

    @Override
    public int readByte() {
        return base_.readByte();
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
    public int readInt() {
        return base_.readInt();
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
    public int skipBytes(int bytes) {
        int pos = base_.beginning();
        int n = base_.end() - pos; // remaining
        if (bytes < n) {
            n = (bytes < -(pos - offset_)) ? -(pos - offset_) : bytes;
        }
        return base_.skipBytes(n);
    }

    @Override
    public int remainingBytes() {
        return base_.remainingBytes();
    }

    @Override
    public void dispose() {
        base_.dispose();
    }

    @Override
    public int spaceBytes() {
        return capacity_ - base_.end();
    }

    @Override
    public int capacityBytes() {
        return capacity_ - offset_;
    }

    @Override
    public int beginning() {
        return base_.beginning() - offset_;
    }

    @Override
    public CodecBuffer beginning(int beginning) {
        base_.beginning(beginning + offset_);
        return this;
    }

    @Override
    public int end() {
        return base_.end() - offset_;
    }

    @Override
    public CodecBuffer end(int end) {
        base_.end(end + offset_);
        return this;
    }

    @Override
    public int drainFrom(CodecBuffer buffer) {
        checkSpace(buffer.remainingBytes());
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
    public void transferTo(ByteBuffer buffer) {
        base_.transferTo(buffer);
    }

    @Override
    public BufferSink addFirst(CodecBuffer buffer) {
        int required = buffer.remainingBytes();
        int frontSpace = base_.beginning() - offset_; // ignore space at tail.
        if (required > frontSpace) {
            throw new IndexOutOfBoundsException(
                    "no space is left. required: " + required + ", front space: " + frontSpace);
        }
        base_.addFirst(buffer);
        return this;
    }

    @Override
    public BufferSink addLast(CodecBuffer buffer) {
        int required = buffer.remainingBytes();
        int backSpace = capacity_ - base_.end(); // ignore space at head.
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
        int frontSpace = base_.beginning() - offset_;
        if (frontSpace == 0) {
            return this;
        }

        if (base_.hasArray()) {
            byte[] b = base_.toArray();
            int os = base_.arrayOffset() + offset_;
            System.arraycopy(b, os + frontSpace, b, os, base_.remainingBytes());
            base_.beginning(offset_);
            base_.end(base_.end() - frontSpace);
        } else {
            ByteBuffer bb = base_.toByteBuffer();
            bb.position(offset_).limit(capacity_);
            ByteBuffer s = bb.slice();
            s.position(frontSpace);
            s.compact();
            base_.beginning(offset_).end(base_.end() - frontSpace);
        }
        return this;
    }

    @Override
    public CodecBuffer clear() {
        base_.beginning(offset_);
        base_.end(offset_);
        return this;
    }

    @Override
    public ByteBuffer toByteBuffer() {
        ByteBuffer bb = base_.toByteBuffer();
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
    public byte[] toArray() {
        return base_.toArray();
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
                + "(beginning:" + beginning() + ", end:" + end() + ", capacity:" + capacityBytes()
                + ", offset:" + offset_ + ')';
    }
}
