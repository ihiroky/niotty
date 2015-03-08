package net.ihiroky.niotty.buffer;

import net.ihiroky.niotty.util.Arguments;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.GatheringByteChannel;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;

/**
 * Implementation of {@link CodecBuffer} using {@code byte[]}.
 */
public class ArrayCodecBuffer extends AbstractCodecBuffer {

    private Chunk<byte[]> chunk_;
    private byte[] buffer_;
    private int start_;
    private int end_;

    ArrayCodecBuffer() {
        this(ArrayChunkFactory.instance(), Buffers.DEFAULT_CAPACITY);
    }

    ArrayCodecBuffer(ChunkManager<byte[]> manager, int initialCapacity) {
        Arguments.requireNonNull(manager, "manager");
        Arguments.requirePositiveOrZero(initialCapacity, "initialCapacity");

        chunk_ = manager.newChunk(initialCapacity);
        buffer_ = chunk_.initialize();
    }

    ArrayCodecBuffer(byte[] b, int offset, int length) {
        Arguments.requireNonNull(b, "b");
        Arguments.requirePositiveOrZero(offset, "offset");
        Arguments.requirePositiveOrZero(length, "length");
        if (offset + length > b.length) {
            throw new IndexOutOfBoundsException(
                    "offset + length (" + (offset + length) + ") exceeds buffer capacity " + b.length);
        }
        ArrayChunk c = new ArrayChunk(b, ArrayChunkFactory.instance());
        c.ready();
        chunk_ = c;
        buffer_ = c.initialize();
        start_ = offset;
        end_ = offset + length;
    }

    private ArrayCodecBuffer(ArrayCodecBuffer b) {
        chunk_ = b.chunk_;
        buffer_ = b.chunk_.retain();
        start_ = b.start_;
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
            int remaining = end_ - start_;
            int minExpandBase = (start_ == 0) ? chunk_.size() : remaining;
            int newCapacity = Math.max(remaining + space, minExpandBase * EXPAND_MULTIPLIER);
            Chunk<byte[]> newChunk = chunk_.reallocate(newCapacity);
            byte[] newBuffer = newChunk.initialize();
            System.arraycopy(buffer_, start_, newBuffer, 0, remaining);
            start_ = 0;
            end_ = remaining;
            chunk_ = newChunk;
            buffer_ = newBuffer;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ArrayCodecBuffer writeByte(int value) {
        ensureSpace(1);
        buffer_[end_++] = (byte) value;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ArrayCodecBuffer writeBytes(byte[] bytes, int offset, int length) {
        ensureSpace(length);
        System.arraycopy(bytes, offset, buffer_, end_, length);
        end_ += length;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public ArrayCodecBuffer writeBytes(ByteBuffer byteBuffer) {
        int remaining = byteBuffer.remaining();
        ensureSpace(remaining);
        byteBuffer.get(buffer_, end_, remaining);
        end_ += remaining;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ArrayCodecBuffer writeShort(int value) {
        ensureSpace(CodecUtil.SHORT_BYTES);
        int c = end_;
        byte[] b = buffer_;
        b[c] = (byte) ((value >>> CodecUtil.BYTE_SHIFT1) & CodecUtil.BYTE_MASK);
        b[c + 1] = (byte) (value & CodecUtil.BYTE_MASK);
        end_ = c + CodecUtil.SHORT_BYTES;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ArrayCodecBuffer writeChar(char value) {
        ensureSpace(CodecUtil.CHAR_BYTES);
        int c = end_;
        byte[] b = buffer_;
        b[c] = (byte) ((value >>> CodecUtil.BYTE_SHIFT1) & CodecUtil.BYTE_MASK);
        b[c + 1] = (byte) (value & CodecUtil.BYTE_MASK);
        end_ = c + CodecUtil.CHAR_BYTES;
        return this;
    }

    @Override
    public ArrayCodecBuffer writeMedium(int value) {
        ensureSpace(CodecUtil.MEDIUM_BYTES);
        int c = end_;
        byte[] b = buffer_;
        int offset = 1;
        b[c] = (byte) ((value >>> CodecUtil.BYTE_SHIFT2) & CodecUtil.BYTE_MASK);
        b[c + offset++] = (byte) ((value >>> CodecUtil.BYTE_SHIFT1) & CodecUtil.BYTE_MASK);
        b[c + offset] = (byte) (value & CodecUtil.BYTE_MASK);
        end_ = c + CodecUtil.MEDIUM_BYTES;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ArrayCodecBuffer writeInt(int value) {
        ensureSpace(CodecUtil.INT_BYTES);
        int c = end_;
        byte[] b = buffer_;
        int offset = 1;
        b[c] = (byte) ((value >>> CodecUtil.BYTE_SHIFT3) & CodecUtil.BYTE_MASK);
        b[c + offset++] = (byte) ((value >>> CodecUtil.BYTE_SHIFT2) & CodecUtil.BYTE_MASK);
        b[c + offset++] = (byte) ((value >>> CodecUtil.BYTE_SHIFT1) & CodecUtil.BYTE_MASK);
        b[c + offset] = (byte) (value & CodecUtil.BYTE_MASK);
        end_ = c + CodecUtil.INT_BYTES;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ArrayCodecBuffer writeLong(long value) {
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
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public ArrayCodecBuffer writeStringContent(String s, CharsetEncoder encoder) {
        Arguments.requireNonNull(encoder, "encoder");
        Arguments.requireNonNull(s, "s");

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
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public byte readByte() {
        if (start_ >= end_) {
            throw new IndexOutOfBoundsException("The start index exceeds end of buffer.");
        }
        return buffer_[start_++];
    }

    @Override
    public int readUnsignedByte() {
        if (start_ >= end_) {
            throw new IndexOutOfBoundsException("The start exceeds end of buffer.");
        }
        return buffer_[start_++] & CodecUtil.BYTE_MASK;
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
        int start = start_;
        int remaining = end_ - start;
        int read = (length <= remaining) ? length : remaining;
        System.arraycopy(buffer_, start, bytes, offset, read);
        start_ += read;
        return read;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int readBytes(ByteBuffer byteBuffer) {
        int space = byteBuffer.remaining();
        int start = start_;
        int remaining = end_ - start;
        int read = (space <= remaining) ? space : remaining;
        byteBuffer.put(buffer_, start_, read);
        start_ += read;
        return read;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public char readChar() {
        if (start_ + CodecUtil.CHAR_BYTES > end_) {
            throw new IndexOutOfBoundsException("The start index exceeds the end index if read char.");
        }
        byte[] b = buffer_;
        return (char) (((b[start_++] & CodecUtil.BYTE_MASK) << CodecUtil.BYTE_SHIFT1)
                | (b[start_++] & CodecUtil.BYTE_MASK));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public short readShort() {
        if (start_ + CodecUtil.SHORT_BYTES > end_) {
            throw new IndexOutOfBoundsException("The start index exceeds the end index if read short.");
        }
        byte[] b = buffer_;
        return (short) (((b[start_++] & CodecUtil.BYTE_MASK) << CodecUtil.BYTE_SHIFT1)
                | (b[start_++] & CodecUtil.BYTE_MASK));
    }

    @Override
    public int readUnsignedMedium() {
        byte[] b = buffer_;
        int pos = start_;
        if (pos + CodecUtil.MEDIUM_BYTES > end_) {
            throw new IndexOutOfBoundsException("The start index exceeds the end index if read medium wide byte.");
        }
        int result = ((b[pos++] & CodecUtil.BYTE_MASK) << CodecUtil.BYTE_SHIFT2)
                | ((b[pos++] & CodecUtil.BYTE_MASK) << CodecUtil.BYTE_SHIFT1)
                |  (b[pos++] & CodecUtil.BYTE_MASK);
        start_ = pos;
        return result;

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int readInt() {
        byte[] b = buffer_;
        int pos = start_;
        if (pos + CodecUtil.INT_BYTES > end_) {
            throw new IndexOutOfBoundsException("The start index exceeds the end index if read int wide byte.");
        }
        int result = ((b[pos++] & CodecUtil.BYTE_MASK) << CodecUtil.BYTE_SHIFT3)
                | ((b[pos++] & CodecUtil.BYTE_MASK) << CodecUtil.BYTE_SHIFT2)
                | ((b[pos++] & CodecUtil.BYTE_MASK) << CodecUtil.BYTE_SHIFT1)
                |  (b[pos++] & CodecUtil.BYTE_MASK);
        start_ = pos;
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long readLong() {
        byte[] b = buffer_;
        int pos = start_;
        if (pos + CodecUtil.LONG_BYTES > end_) {
            throw new IndexOutOfBoundsException("The start index exceeds the end index if read long wide byte.");
        }
        long result = ((((long) b[pos++] & CodecUtil.BYTE_MASK) << CodecUtil.BYTE_SHIFT7)
                | (((long) b[pos++] & CodecUtil.BYTE_MASK) << CodecUtil.BYTE_SHIFT6)
                | (((long) b[pos++] & CodecUtil.BYTE_MASK) << CodecUtil.BYTE_SHIFT5)
                | (((long) b[pos++] & CodecUtil.BYTE_MASK) << CodecUtil.BYTE_SHIFT4)
                | (((long) b[pos++] & CodecUtil.BYTE_MASK) << CodecUtil.BYTE_SHIFT3)
                | (((long) b[pos++] & CodecUtil.BYTE_MASK) << CodecUtil.BYTE_SHIFT2)
                | (((long) b[pos++] & CodecUtil.BYTE_MASK) << CodecUtil.BYTE_SHIFT1)
                |  ((long) b[pos++] & CodecUtil.BYTE_MASK));
        start_ = pos;
        return result;
    }

    @Override
    public String readStringContent(CharsetDecoder decoder, int length) {
        String cached = StringCache.getCachedValue(this, decoder, length);
        if (cached != null) {
            return cached;
        }

        ByteBuffer input = ByteBuffer.wrap(buffer_, start_, length);
        CharBuffer output = CharBuffer.allocate(Buffers.outputCharBufferSize(decoder, length));
        for (;;) {
            CoderResult cr = decoder.decode(input, output, true);
            if (cr.isUnderflow()) {
                cr = decoder.flush(output);
                if (cr.isUnderflow()) {
                    start_ = input.position();
                    decoder.reset();
                    break;
                }
            }
            if (cr.isOverflow()) {
                output = Buffers.expand(output, decoder, input.remaining());
                continue;
            }
            if (cr.isError()) {
                start_ = input.position();
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
    public int skipStartIndex(int n) {
        int pos = start_;
        int r = end_ - pos; // remaining
        if (n < r) {
            r = (n < -pos) ? -pos : n;
        }
        start_ += r;
        return r;
    }

    @Override
    public int skipEndIndex(int n) {
        int pos = end_;
        int s = buffer_.length - pos; // space
        if (n < s) {
            int r = pos - start_; // remaining
            s = (n < -r) ? -r : n;
        }
        end_ += s;
        return s;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean sink(GatheringByteChannel channel) throws IOException {
        int remaining = end_ - start_;
        if (remaining == 0) {
            return true;
        }

        ByteBuffer byteBuffer = ByteBuffer.wrap(buffer_, start_, remaining);
        int writeBytes = channel.write(byteBuffer);
        start_ += writeBytes;
        return writeBytes == remaining;
    }

    @Override
    public boolean sink(DatagramChannel channel, ByteBuffer buffer, SocketAddress target) throws IOException {
        int remaining = end_ - start_;
        if (remaining == 0) {
            return true;
        }

        buffer.put(buffer_, start_, remaining).flip();
        boolean sent = (channel.send(buffer, target) == remaining);
        buffer.clear();
        return sent;
    }

    @Override
    public void copyTo(ByteBuffer byteBuffer) {
        int start = start_;
        int remaining = end_ - start;
        byteBuffer.put(buffer_, start_, remaining);
    }

    @Override
    public ArrayCodecBuffer addFirst(CodecBuffer buffer) {
        Arguments.requireNonNull(buffer, "buffer");
        int inputSize = buffer.remaining();
        if (inputSize == 0) {
            return this;
        }

        int beginning = start_;
        int frontSpace = beginning;

        if (frontSpace < inputSize) {
            int wantedBytes = inputSize - frontSpace;
            if (space() >= wantedBytes) {
                System.arraycopy(buffer_, beginning, buffer_, beginning + wantedBytes, remaining());
                beginning += wantedBytes;
                end_ += wantedBytes;
            } else {
                int remaining = remaining();
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
            System.arraycopy(buffer.array(), buffer.startIndex(), buffer_, beginning - inputSize, inputSize);
        } else {
            buffer.readBytes(buffer_, beginning - inputSize, inputSize);
        }
        start_ = beginning - inputSize;
        return this;
    }

    @Override
    public ArrayCodecBuffer addLast(CodecBuffer buffer) {
        Arguments.requireNonNull(buffer, "buffer");
        int inputSize = buffer.remaining();
        if (inputSize == 0) {
            return this;
        }

        int backSpace = space();
        if (inputSize > backSpace) {
            int remaining = remaining();
            int frontSpace = start_;
            int required = inputSize - backSpace;
            if (frontSpace >= required) {
                System.arraycopy(buffer_, start_, buffer_, 0, remaining);
                start_ -= frontSpace;
                end_ -= frontSpace;
            } else {
                int newCapacity = Math.max(remaining * EXPAND_MULTIPLIER, remaining + inputSize);
                Chunk<byte[]> newChunk = chunk_.reallocate(newCapacity);
                byte[] newBuffer = newChunk.initialize();
                System.arraycopy(buffer_, start_, newBuffer, 0, remaining);
                start_ = 0;
                end_ = remaining;
                chunk_ = newChunk;
                buffer_ = newBuffer;
            }
        }
        if (buffer.hasArray()) {
            System.arraycopy(buffer.array(), buffer.startIndex(), buffer_, end_, inputSize);
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
    public int remaining() {
        return end_ - start_;
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
    public int space() {
        return buffer_.length - end_;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int capacity() {
        return buffer_.length;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int startIndex() {
        return start_;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CodecBuffer startIndex(int beginning) {
        if (beginning < 0) {
            throw new IndexOutOfBoundsException("startIndex is negative.");
        }
        if (beginning > end_) {
            throw new IndexOutOfBoundsException("startIndex is greater than endIndex.");
        }
        start_ = beginning;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int endIndex() {
        return end_;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CodecBuffer endIndex(int end) {
        if (end < start_) {
            throw new IndexOutOfBoundsException("The endIndex " + end + " is less than startIndex " + start_ + ".");
        }
        if (end > buffer_.length) {
            throw new IndexOutOfBoundsException("The endIndex " + end+ " is greater than capacity " + buffer_.length + ".");
        }
        end_ = end;
        return this;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public CodecBuffer clear() {
        start_ = 0;
        end_ = 0;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ByteBuffer byteBuffer() {
        return ByteBuffer.wrap(buffer_, start_, (end_ - start_));
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
        int fromIndexInContent = fromIndex + start_;
        if (fromIndexInContent >= end) {
            return -1;
        }

        byte[] buffer = buffer_;
        for (int i = fromIndexInContent; i < end; i++) {
            if (buffer[i] == bb) {
                return i - start_;
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
        int fromIndexInContent = fromIndex + start_;
        if (fromIndexInContent >= end) {
            return -1;
        }

        byte[] buffer = buffer_;
        int e = end - b.length;
        BUFFER_LOOP: for (int i = fromIndexInContent; i <= e; i++) {
            if (buffer[i] != b[0]) {
                continue;
            }
            for (int bi = 1; bi < b.length; bi++) {
                if (buffer[i + bi] != b[bi]) {
                    continue BUFFER_LOOP;
                }
            }
            return i - start_;
        }
        return -1;
    }

    @Override
    public int lastIndexOf(int b, int fromIndex) {
        byte bb = (byte) b;
        int start = start_;
        int end = end_;

        int fromIndexInContent = fromIndex + start;
        if (fromIndexInContent < start) {
            return -1;
        }
        if (fromIndexInContent >= end) {
            fromIndexInContent = end - 1;
        }


        byte[] buffer = buffer_;
        for (int i = fromIndexInContent; i >= start; i--) {
            if (buffer[i] == bb) {
                return i - start;
            }
        }
        return -1;
    }

    @Override
    public int lastIndexOf(byte[] b, int fromIndex) {
        if (b == null || b.length == 0 || fromIndex < 0) {
            return -1;
        }

        int start = start_;
        int end = end_;
        int fromIndexInContent = fromIndex + start;
        if (fromIndexInContent > end - b.length) {
            fromIndexInContent = end - b.length;
        }


        byte[] buffer = buffer_;
        BUFFER_LOOP: for (int i = fromIndexInContent; i >= start; i--) {
            if (buffer[i] != b[0]) {
                continue;
            }
            for (int bi = 1; bi < b.length; bi++) {
                if (buffer[i + bi] != b[bi]) {
                    continue BUFFER_LOOP;
                }
            }
            return i - start;
        }
        return -1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int drainFrom(CodecBuffer buffer) {
        return drainFromNoCheck(buffer, buffer.remaining());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int drainFrom(CodecBuffer buffer, int bytes) {
        if (bytes < 0) {
            throw new IllegalArgumentException("bytes must be >= 0.");
        }
        int remaining = buffer.remaining();
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
        if (bytes <= 0 || bytes > remaining()) {
            throw new IllegalArgumentException("Invalid input " + bytes + ". " + remaining() + " byte remains.");
        }
        CodecBuffer sliced = new SlicedCodecBuffer(duplicate(), bytes);
        start_ += bytes;
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
        int beginning = start_;
        if (beginning == 0) {
            return this;
        }

        System.arraycopy(buffer_, beginning, buffer_, 0, end_ - beginning);
        end_ -= beginning;
        start_ = 0;
        return this;
    }

    /**
     * Returns a summary of this buffer state.
     * @return a summary of this buffer state
     */
    @Override
    public String toString() {
        return ArrayCodecBuffer.class.getName()
                + "(startIndex:" + start_ + ", endIndex:" + end_ + ", capacity:" + buffer_.length + ')';
    }

    @Override
    public boolean equals(Object object) {
        if (object == this) {
            return true;
        }
        if (object instanceof ArrayCodecBuffer) {
            ArrayCodecBuffer that = (ArrayCodecBuffer) object;
            int r = this.remaining();
            if (r == that.remaining()) {
                int s0 = this.start_;
                int s1 = that.start_;
                for (int i = 0; i < r; i++) {
                    if (this.buffer_[s0 + i] != that.buffer_[s1 + i]) {
                        return false;
                    }
                }
            }
            return true;
        }
        return super.equals(object);
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
