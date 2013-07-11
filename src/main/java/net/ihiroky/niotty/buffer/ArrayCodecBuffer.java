package net.ihiroky.niotty.buffer;

import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.GatheringByteChannel;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.util.Objects;

/**
 * Implementation of {@link net.ihiroky.niotty.buffer.CodecBuffer} using {@code byte[]}.
 *
 * @author Hiroki Itoh
 */
public class ArrayCodecBuffer extends AbstractCodecBuffer {

    private Chunk<byte[]> chunk_;
    private byte[] buffer_;
    private int beginning_;
    private int end_;

    ArrayCodecBuffer() {
        this(ArrayChunkFactory.instance(), Buffers.DEFAULT_CAPACITY);
    }

    ArrayCodecBuffer(ChunkManager<byte[]> manager, int initialCapacity) {
        Objects.requireNonNull(manager, "manager");
        if (initialCapacity < 0) {
            throw new IllegalArgumentException("initialCapacity must not be negative.");
        }
        chunk_ = manager.newChunk(initialCapacity);
        buffer_ = chunk_.initialize();
    }

    ArrayCodecBuffer(byte[] b, int beginning, int length) {
        Objects.requireNonNull(b, "b");
        if (beginning < 0) {
            throw new IllegalArgumentException("beginning must be zero or positive.");
        }
        if (length < 0) {
            throw new IllegalArgumentException("length must be zero or positive.");
        }
        if (beginning + length > b.length) {
            throw new IndexOutOfBoundsException(
                    "offset + length (" + (beginning + length) + ") exceeds buffer capacity " + b.length);
        }
        ArrayChunk c = new ArrayChunk(b, ArrayChunkFactory.instance());
        c.ready();
        chunk_ = c;
        buffer_ = c.initialize();
        beginning_ = beginning;
        end_ = beginning + length;
    }

    private ArrayCodecBuffer(ArrayCodecBuffer b) {
        chunk_ = b.chunk_;
        buffer_ = b.chunk_.retain();
        beginning_ = b.beginning_;
        end_ = b.end_;
    }

    /**
     * Ensures the backed byte array capacity. The new capacity is the large of the two, sum of the current position
     * and {@code length}, and twice the size of current capacity.
     *
     * @param space the size of byte to be written
     */
    void ensureSpace(int space) {
        int mySpace = buffer_.length - end_;
        if (space > mySpace) {
            int remaining = end_ - beginning_;
            int minExpandBase = (beginning_ == 0) ? chunk_.size() : remaining;
            int newCapacity = Math.max(remaining + space, minExpandBase * EXPAND_MULTIPLIER);
            Chunk<byte[]> newChunk = chunk_.reallocate(newCapacity);
            byte[] newBuffer = newChunk.initialize();
            System.arraycopy(buffer_, beginning_, newBuffer, 0, remaining);
            beginning_ = 0;
            end_ = remaining;
            chunk_ = newChunk;
            buffer_ = newBuffer;
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
    public void writeShort(short value) {
        ensureSpace(CodecUtil.SHORT_BYTES);
        int c = end_;
        byte[] b = buffer_;
        b[c] = (byte) ((value >>> CodecUtil.BYTE_SHIFT1) & CodecUtil.BYTE_MASK);
        b[c + 1] = (byte) (value & CodecUtil.BYTE_MASK);
        end_ = c + CodecUtil.SHORT_BYTES;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeChar(char value) {
        ensureSpace(CodecUtil.CHAR_BYTES);
        int c = end_;
        byte[] b = buffer_;
        b[c] = (byte) ((value >>> CodecUtil.BYTE_SHIFT1) & CodecUtil.BYTE_MASK);
        b[c + 1] = (byte) (value & CodecUtil.BYTE_MASK);
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
    public void writeString(String s, CharsetEncoder encoder) {
        Objects.requireNonNull(encoder, "encoder");
        Objects.requireNonNull(s, "s");

        int end = end_;
        CharBuffer input = CharBuffer.wrap(s);
        ByteBuffer output = ByteBuffer.wrap(buffer_, end, buffer_.length - end);
        for (;;) {
            CoderResult cr = encoder.encode(input, output, true);
            if (cr.isUnderflow()) {
                cr = encoder.flush(output);
                if (cr.isUnderflow()) {
                    end_ = output.position();
                    encoder.reset();
                    break;
                }
            }
            if (cr.isOverflow()) {
                end = output.position();
                end_ = end;
                ensureSpace(Buffers.outputByteBufferSize(encoder, input.remaining()));
                output = ByteBuffer.wrap(buffer_, end, buffer_.length - end);
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
    public int readBytes(byte[] bytes, int offset, int length) {
        if (bytes.length < offset + length) {
            throw new IllegalArgumentException(
                    "length of bytes is too short to read " + length + " bytes from offset " + offset + ".");
        }
        int beginning = beginning_;
        int remaining = end_ - beginning;
        int read = (length <= remaining) ? length : remaining;
        System.arraycopy(buffer_, beginning, bytes, offset, read);
        beginning_ += read;
        return read;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int readBytes(ByteBuffer byteBuffer) {
        int space = byteBuffer.remaining();
        int beginning = beginning_;
        int remaining = end_ - beginning;
        int read = (space <= remaining) ? space : remaining;
        byteBuffer.put(buffer_, beginning_, read);
        beginning_ += read;
        return read;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public char readChar() {
        if (beginning_ + CodecUtil.CHAR_BYTES > end_) {
            throw new IndexOutOfBoundsException("position exceeds the end of buffer if read char.");
        }
        byte[] b = buffer_;
        return (char) (((b[beginning_++] & CodecUtil.BYTE_MASK) << CodecUtil.BYTE_SHIFT1)
                | (b[beginning_++] & CodecUtil.BYTE_MASK));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public short readShort() {
        if (beginning_ + CodecUtil.SHORT_BYTES > end_) {
            throw new IndexOutOfBoundsException("position exceeds the end of buffer if read short.");
        }
        byte[] b = buffer_;
        return (short) (((b[beginning_++] & CodecUtil.BYTE_MASK) << CodecUtil.BYTE_SHIFT1)
                | (b[beginning_++] & CodecUtil.BYTE_MASK));
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

    @Override
    public String readString(CharsetDecoder decoder, int bytes) {
        String cached = StringCache.getCachedValue(this, decoder, bytes);
        if (cached != null) {
            return cached;
        }

        ByteBuffer input = ByteBuffer.wrap(buffer_, beginning_, bytes);
        CharBuffer output = CharBuffer.allocate(Buffers.outputCharBufferSize(decoder, bytes));
        for (;;) {
            CoderResult cr = decoder.decode(input, output, true);
            if (cr.isUnderflow()) {
                cr = decoder.flush(output);
                if (cr.isUnderflow()) {
                    beginning_ = input.position();
                    decoder.reset();
                    break;
                }
            }
            if (cr.isOverflow()) {
                output = Buffers.expand(output, decoder, input.remaining());
                continue;
            }
            if (cr.isError()) {
                beginning_ = input.position();
                Buffers.throwRuntimeException(cr);
            }
        }
        output.flip();
        return StringCache.toString(output, decoder);
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
    public boolean transferTo(GatheringByteChannel channel) throws IOException {
        int remaining = end_ - beginning_;
        if (remaining == 0) {
            return true;
        }

        ByteBuffer byteBuffer = ByteBuffer.wrap(buffer_, beginning_, remaining);
        int writeBytes = channel.write(byteBuffer);
        beginning_ += writeBytes;
        return writeBytes == remaining;
    }

    @Override
    public void transferTo(ByteBuffer byteBuffer) {
        int space = byteBuffer.remaining();
        int beginning = beginning_;
        int remaining = end_ - beginning;
        if (space < remaining) {
            throw new BufferOverflowException();
        }
        byteBuffer.put(buffer_, beginning_, remaining);
    }

    @Override
    public ArrayCodecBuffer addFirst(CodecBuffer buffer) {
        Objects.requireNonNull(buffer, "buffer");
        int inputSize = buffer.remainingBytes();
        if (inputSize == 0) {
            return this;
        }

        int beginning = beginning_;
        int frontSpace = beginning;

        if (frontSpace < inputSize) {
            int wantedBytes = inputSize - frontSpace;
            if (spaceBytes() >= wantedBytes) {
                System.arraycopy(buffer_, beginning, buffer_, beginning + wantedBytes, remainingBytes());
                beginning += wantedBytes;
                end_ += wantedBytes;
            } else {
                int remaining = remainingBytes();
                int newEnd = remaining + inputSize;
                int newCapacity = Math.max(remaining * EXPAND_MULTIPLIER, newEnd);
                Chunk<byte[]> newChunk = chunk_.reallocate(newCapacity);
                byte[] newBuffer = newChunk.initialize();
                System.arraycopy(buffer_, beginning, newBuffer, inputSize, remaining);
                beginning = inputSize;
                end_ = newEnd;
                chunk_ = newChunk;
                buffer_ = newBuffer;
            }
        }

        if (buffer.hasArray()) {
            System.arraycopy(buffer.array(), buffer.beginning(), buffer_, beginning - inputSize, inputSize);
        } else {
            buffer.readBytes(buffer_, beginning - inputSize, inputSize);
        }
        beginning_ = beginning - inputSize;
        return this;
    }

    @Override
    public ArrayCodecBuffer addLast(CodecBuffer buffer) {
        Objects.requireNonNull(buffer, "buffer");
        int inputSize = buffer.remainingBytes();
        if (inputSize == 0) {
            return this;
        }

        int backSpace = spaceBytes();
        if (inputSize > backSpace) {
            int remaining = remainingBytes();
            int frontSpace = beginning_;
            int required = inputSize - backSpace;
            if (frontSpace >= required) {
                System.arraycopy(buffer_, beginning_, buffer_, 0, remaining);
                beginning_ -= frontSpace;
                end_ -= frontSpace;
            } else {
                int newCapacity = Math.max(remaining * EXPAND_MULTIPLIER, remaining + inputSize);
                Chunk<byte[]> newChunk = chunk_.reallocate(newCapacity);
                byte[] newBuffer = newChunk.initialize();
                System.arraycopy(buffer_, beginning_, newBuffer, 0, remaining);
                beginning_ = 0;
                end_ = remaining;
                chunk_ = newChunk;
                buffer_ = newBuffer;
            }
        }
        if (buffer.hasArray()) {
            System.arraycopy(buffer.array(), buffer.beginning(), buffer_, end_, inputSize);
        } else {
            buffer.readBytes(buffer_, end_, inputSize);
        }
        end_ += inputSize;
        return this;
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
    public void dispose() {
        // TODO discuss whether flag to control the call chunk_.release only once is needed.
        chunk_.release();
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
        end_ = 0;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ByteBuffer byteBuffer() {
        return ByteBuffer.wrap(buffer_, beginning_, (end_ - beginning_));
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
    public byte[] array() {
        return buffer_;
    }

    /**
     * {@inheritDoc}
     */
    public int arrayOffset() {
        return 0;
    }

    @Override
    public int indexOf(int b, int fromIndex) {
        byte bb = (byte) b;
        int end = end_;

        if (fromIndex < 0) {
            fromIndex = 0;
        }
        int fromIndexInContent = fromIndex + beginning_;
        if (fromIndexInContent >= end) {
            return -1;
        }

        byte[] buffer = buffer_;
        for (int i = fromIndexInContent; i < end; i++) {
            if (buffer[i] == bb) {
                return i - beginning_;
            }
        }
        return -1;
    }

    @Override
    public int indexOf(byte[] b, int fromIndex) {
        if (b == null || b.length == 0) {
            return -1;
        }

        int end = end_;
        if (fromIndex < 0) {
            fromIndex = 0;
        }
        int fromIndexInContent = fromIndex + beginning_;
        if (fromIndexInContent >= end) {
            return -1;
        }

        byte[] buffer = buffer_;
        int e = end - b.length;
        for (int i = fromIndexInContent; i <= e; i++) {
            if (buffer[i] != b[0]) {
                continue;
            }
            for (int bi = 1; bi < b.length; bi++) {
                if (buffer[i + bi] == b[bi]) {
                    return i - beginning_;
                }
            }
        }
        return -1;
    }

    @Override
    public int lastIndexOf(int b, int fromIndex) {
        byte bb = (byte) b;
        int beginning = beginning_;
        int end = end_;

        int fromIndexInContent = fromIndex + beginning;
        if (fromIndexInContent < beginning) {
            return -1;
        }
        if (fromIndexInContent >= end) {
            fromIndexInContent = end - 1;
        }


        byte[] buffer = buffer_;
        for (int i = fromIndexInContent; i >= beginning; i--) {
            if (buffer[i] == bb) {
                return i - beginning;
            }
        }
        return -1;
    }

    @Override
    public int lastIndexOf(byte[] b, int fromIndex) {
        if (b == null || b.length == 0 || fromIndex < 0) {
            return -1;
        }

        int beginning = beginning_;
        int end = end_;
        int fromIndexInContent = fromIndex + beginning;
        if (fromIndexInContent > end - b.length) {
            fromIndexInContent = end - b.length;
        }


        byte[] buffer = buffer_;
        BUFFER_LOOP: for (int i = fromIndexInContent; i >= beginning; i--) {
            if (buffer[i] != b[0]) {
                continue;
            }
            for (int bi = 1; bi < b.length; bi++) {
                if (buffer[i + bi] != b[bi]) {
                    continue BUFFER_LOOP;
                }
            }
            return i - beginning;
        }
        return -1;
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
        if (bytes <= 0 || bytes > remainingBytes()) {
            throw new IllegalArgumentException("Invalid input " + bytes + ". " + remainingBytes() + " byte remains.");
        }
        CodecBuffer sliced = new SlicedCodecBuffer(duplicate(), bytes);
        beginning_ += bytes;
        return sliced;
    }

    @Override
    public CodecBuffer slice() {
        return new SlicedCodecBuffer(duplicate());
    }

    @Override
    public CodecBuffer duplicate() {
        return new ArrayCodecBuffer(this);
    }

    @Override
    public CodecBuffer compact() {
        int beginning = beginning_;
        if (beginning == 0) {
            return this;
        }

        System.arraycopy(buffer_, beginning, buffer_, 0, end_ - beginning);
        end_ -= beginning;
        beginning_ = 0;
        return this;
    }

    /**
     * Returns a summary of this buffer state.
     * @return a summary of this buffer state
     */
    @Override
    public String toString() {
        return ArrayCodecBuffer.class.getName()
                + "(beginning:" + beginning_ + ", end:" + end_ + ", capacity:" + buffer_.length + ')';
    }

    /**
     * Returns a reference count of a chunk which manages an internal byte array.
     * @return the reference count
     */
    public int referenceCount() {
        return chunk_.referenceCount();
    }

    Chunk<byte[]> chunk() {
        return chunk_;
    }
}
