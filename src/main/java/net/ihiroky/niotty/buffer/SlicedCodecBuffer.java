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
 * @author Hiroki Itoh
 */
public class SlicedCodecBuffer extends AbstractCodecBuffer {

    final CodecBuffer base_;
    final int offset_;
    final int capacity_; // fix capacity to avoid region over wrap.

    SlicedCodecBuffer(CodecBuffer base) {
        CodecBuffer b = base.duplicate();
        base_ = b;
        offset_ = b.beginning();
        capacity_ = b.end();
    }

    SlicedCodecBuffer(CodecBuffer base, int bytes) {
        int beginning = base.beginning();
        int capacity = beginning + bytes;
        if (capacity > base.end()) {
            throw new IllegalArgumentException("capacity must be less than or equal base.end().");
        }

        CodecBuffer b = base.duplicate();
        b.end(capacity);
        base_ = b;
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
    public int priority() {
        return base_.priority();
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
}
