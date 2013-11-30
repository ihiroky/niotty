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
 * Implementation of {@link CodecBuffer} using {@code java.nio.ByteBuffer}.
 * @author Hiroki Itoh
 */
public class ByteBufferCodecBuffer extends AbstractCodecBuffer {

    private Chunk<ByteBuffer> chunk_;
    private ByteBuffer buffer_;
    private int start_;
    private int end_;
    private Mode mode_;

    /**
     * buffer r/w mode expression.
     */
    enum Mode {
        /** read mode. */
        READ,
        /** write mode. */
        WRITE
    }

    ByteBufferCodecBuffer() {
        this(ByteBufferChunkFactory.heap(), Buffers.DEFAULT_CAPACITY);
    }

    ByteBufferCodecBuffer(ChunkManager<ByteBuffer> manager, int initialCapacity) {
        chunk_ = manager.newChunk(initialCapacity);
        buffer_ = chunk_.initialize();
        mode_ = Mode.WRITE;
    }

    ByteBufferCodecBuffer(ByteBuffer buffer, boolean cleanOnDispose) {
        Arguments.requireNonNull(buffer, "buffer");

        ByteBufferChunkFactory chunkManager = buffer.isDirect()
                ? ByteBufferChunkFactory.direct(cleanOnDispose)
                : ByteBufferChunkFactory.heap();
        ByteBufferChunk c = new ByteBufferChunk(buffer, chunkManager);
        c.ready();
        chunk_ = c;
        buffer_ = chunk_.initialize();
        start_ = buffer.position();
        end_ = buffer.limit();
        mode_ = Mode.READ;
    }

    private ByteBufferCodecBuffer(ByteBufferCodecBuffer b) {
        chunk_ = b.chunk_;
        buffer_ = b.chunk_.retain();
        start_ = b.start_;
        end_ = b.end_;
        mode_ = b.mode_;
    }

    private void changeModeToWrite() {
        if (mode_ == Mode.WRITE) {
            end_ = buffer_.position();
        } else {
            mode_ = Mode.WRITE;
            ByteBuffer b = buffer_;
            start_ = b.position();
            end_ = b.limit();
            b.limit(b.capacity());
            b.position(end_);
        }
    }

    private void changeModeToRead() {
        if (mode_ == Mode.READ) {
            start_ = buffer_.position();
        } else {
            mode_ = Mode.READ;
            ByteBuffer b = buffer_;
            end_ = b.position();
            b.position(start_);
            b.limit(end_);
        }
    }

    private void syncBeginEnd() {
        if (mode_ == Mode.WRITE) {
            end_ = buffer_.position();
        } else {
            start_ = buffer_.position();
        }
    }

    /**
     * Ensures the backed byte array capacity. The new capacity is the large of the two, sum of the current position
     * and {@code length}, and twice the size of current capacity.
     *
     * @param space the size of byte to be written
     */
    void ensureSpace(int space) {
        syncBeginEnd();
        ByteBuffer bb = buffer_;
        int mySpace = bb.capacity() - end_;
        if (space <= mySpace) {
            return;
        }

        int remaining = end_ - start_;
        int minExpandBase = (start_ == 0) ? buffer_.capacity() : remaining;
        int newCapacity = Math.max(remaining + space, minExpandBase * EXPAND_MULTIPLIER);
        Chunk<ByteBuffer> newChunk = chunk_.reallocate(newCapacity);
        ByteBuffer newBuffer = newChunk.initialize();
        bb.position(start_).limit(end_);
        newBuffer.put(bb);
        start_ = 0;
        end_ = newBuffer.position();
        chunk_ = newChunk;
        buffer_ = newBuffer;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ByteBufferCodecBuffer writeByte(int value) {
        changeModeToWrite();
        ensureSpace(1);
        buffer_.put((byte) value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ByteBufferCodecBuffer writeBytes(byte[] bytes, int offset, int length) {
        changeModeToWrite();
        ensureSpace(length);
        buffer_.put(bytes, offset, length);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public ByteBufferCodecBuffer writeBytes(ByteBuffer byteBuffer) {
        changeModeToWrite();
        ensureSpace(byteBuffer.remaining());
        buffer_.put(byteBuffer);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ByteBufferCodecBuffer writeShort(int value) {
        changeModeToWrite();
        ensureSpace(CodecUtil.SHORT_BYTES);
        buffer_.putShort((short) value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ByteBufferCodecBuffer writeChar(char value) {
        changeModeToWrite();
        ensureSpace(CodecUtil.CHAR_BYTES);
        buffer_.putChar(value);
        return this;
    }

    @Override
    public ByteBufferCodecBuffer writeMedium(int value) {
        changeModeToWrite();
        ensureSpace(CodecUtil.MEDIUM_BYTES);

        // forcibly big endian.
        buffer_.put((byte) ((value >>> CodecUtil.BYTE_SHIFT2) & CodecUtil.BYTE_MASK));
        buffer_.put((byte) ((value >>> CodecUtil.BYTE_SHIFT1) & CodecUtil.BYTE_MASK));
        buffer_.put((byte) (value & CodecUtil.BYTE_MASK));
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ByteBufferCodecBuffer writeInt(int value) {
        changeModeToWrite();
        ensureSpace(CodecUtil.INT_BYTES);
        buffer_.putInt(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ByteBufferCodecBuffer writeLong(long value) {
        changeModeToWrite();
        ensureSpace(CodecUtil.LONG_BYTES);
        buffer_.putLong(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public ByteBufferCodecBuffer writeString(String s, CharsetEncoder encoder) {
        Arguments.requireNonNull(s, "s");
        Arguments.requireNonNull(encoder, "encoder");

        changeModeToWrite();
        CharBuffer input = CharBuffer.wrap(s);
        ByteBuffer output = buffer_;
        for (;;) {
            CoderResult cr = encoder.encode(input, output, true);
            if (cr.isUnderflow()) {
                cr = encoder.flush(output);
                if (cr.isUnderflow()) {
                    encoder.reset();
                    break;
                }
            }
            if (cr.isOverflow()) {
                ensureSpace(Buffers.outputByteBufferSize(encoder, input.remaining()));
                output = buffer_;
                continue;
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

    /**
     * {@inheritDoc}
     */
    @Override
    public byte readByte() {
        changeModeToRead();
        return buffer_.get();
    }

    @Override
    public int readUnsignedByte() {
        changeModeToRead();
        return buffer_.get() & CodecUtil.BYTE_MASK;
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
        changeModeToRead();
        ByteBuffer myBuffer = buffer_;
        int remaining = myBuffer.remaining();
        int read = (length >= remaining) ? remaining : length;
        myBuffer.get(bytes, offset, read);
        return read;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int readBytes(ByteBuffer byteBuffer) {
        changeModeToRead();
        ByteBuffer myBuffer = buffer_;
        int space = byteBuffer.remaining();
        int remaining = myBuffer.remaining();
        if (space >= remaining) {
            byteBuffer.put(myBuffer);
            return remaining;
        }
        int limit = myBuffer.limit();
        myBuffer.limit(myBuffer.position() + space);
        byteBuffer.put(myBuffer);
        myBuffer.limit(limit);
        return space;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public char readChar() {
        changeModeToRead();
        return buffer_.getChar();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public short readShort() {
        changeModeToRead();
        return buffer_.getShort();
    }

    @Override
    public int readUnsignedMedium() {
        changeModeToRead();
        ByteBuffer b = buffer_;
        return ((b.get() & CodecUtil.BYTE_MASK) << CodecUtil.BYTE_SHIFT2)
                | ((b.get() & CodecUtil.BYTE_MASK) << CodecUtil.BYTE_SHIFT1)
                |  (b.get() & CodecUtil.BYTE_MASK);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int readInt() {
        changeModeToRead();
        return buffer_.getInt();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long readLong() {
        changeModeToRead();
        return buffer_.getLong();
    }

    @Override
    public String readString(CharsetDecoder decoder, int bytes) {
        changeModeToRead();
        String cached = StringCache.getCachedValue(this, decoder, bytes);
        if (cached != null) {
            return cached;
        }

        ByteBuffer input = buffer_;
        CharBuffer output = CharBuffer.allocate(Buffers.outputCharBufferSize(decoder, bytes));
        int limit = input.limit();
        input.limit(input.position() + bytes);
        for (;;) {
            CoderResult cr = decoder.decode(input, output, true);
            if (cr.isUnderflow()) {
                cr = decoder.flush(output);
                if (cr.isUnderflow()) {
                    input.limit(limit);
                    decoder.reset();
                    break;
                }
            }
            if (cr.isOverflow()) {
                output = Buffers.expand(output, decoder, input.remaining());
                continue;
            }
            if (cr.isError()) {
                input.limit(limit);
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
        changeModeToRead();
        ByteBuffer b = buffer_;
        int r = b.remaining();
        if (n < r) {
            int pos = b.position();
            r = (n < -pos) ? -pos : n;
        }
        b.position(b.position() + r);
        return r;
    }

    @Override
    public int skipEndIndex(int n) {
        changeModeToWrite();
        ByteBuffer b = buffer_;
        int s = b.remaining();
        if (n < s) {
            int r = end_ - start_;
            s = (n < -r) ? -r : n;
        }
        b.position(b.position() + s);
        return s;
    }

    @Override
    public boolean sink(GatheringByteChannel channel) throws IOException {
        changeModeToRead();

        ByteBuffer buffer = buffer_;
        int remaining = buffer.remaining();
        if (remaining == 0) {
            return true;
        }
        int writeBytes = channel.write(buffer);
        return writeBytes == remaining;
    }

    @Override
    public boolean sink(DatagramChannel channel, ByteBuffer buffer, SocketAddress target) throws IOException {
        changeModeToRead();

        int remaining = end_ - start_;
        if (remaining == 0) {
            return true;
        }

        boolean sent;
        if (buffer_.isDirect() || !buffer.isDirect()) {
            sent = (channel.send(buffer_, target) == remaining);
        } else {
            int position = buffer.position();
            buffer.put(buffer_).flip();
            buffer_.position(position);
            sent = (channel.send(buffer, target) == remaining);
            buffer.clear();
        }
        return sent;
    }

    @Override
    public void copyTo(ByteBuffer byteBuffer) {
        changeModeToRead();

        ByteBuffer myBuffer = buffer_;
        int position = myBuffer.position();
        byteBuffer.put(myBuffer);
        myBuffer.position(position);
    }

    @Override
    public ByteBufferCodecBuffer addFirst(CodecBuffer buffer) {
        Arguments.requireNonNull(buffer, "buffer");
        int inputSize = buffer.remaining();
        if (inputSize == 0) {
            return this;
        }

        changeModeToRead();
        ByteBuffer b = buffer_;
        ByteBuffer inputBuffer = buffer.byteBuffer();
        int position = b.position();

        int start;
        if (position >= inputSize) {
            start = position - inputBuffer.remaining();
        } else {
            int wanted = inputSize - position;
            int backSpace = b.capacity() - b.limit();
            int remaining = b.remaining();
            if (wanted <= backSpace) {
                if (b.hasArray()) {
                    byte[] a = b.array();
                    int aos = b.arrayOffset() + position;
                    System.arraycopy(a, aos, a, aos + wanted, remaining);
                    b.limit(position + remaining + wanted);
                } else {
                    byte[] a = new byte[remaining];
                    b.get(a, 0, remaining);
                    b.position(inputSize);
                    b.put(a, 0, remaining);
                }
            } else {
                int newCapacity = Math.max(remaining * EXPAND_MULTIPLIER, remaining + inputSize);
                Chunk<ByteBuffer> newChunk = chunk_.reallocate(newCapacity);
                b = newChunk.initialize();
                b.position(inputSize);
                b.put(buffer_);
                b.position(0);
                b.limit(inputSize + remaining);
                chunk_ = newChunk;
                buffer_ = b;
            }
            end_ = inputSize + remaining;
            start_ = 0;
            start = 0;
        }

        b.position(start);
        b.put(inputBuffer);
        b.position(start);
        return this;
    }

    @Override
    public ByteBufferCodecBuffer addLast(CodecBuffer buffer) {
        Arguments.requireNonNull(buffer, "buffer");
        int inputSize = buffer.remaining();
        if (inputSize == 0) {
            return this;
        }

        syncBeginEnd();
        ByteBuffer inputBuffer = buffer.byteBuffer();
        ByteBuffer cb = buffer_;
        int backSpace = cb.capacity() - end_;
        if (inputSize >= backSpace) {
            changeModeToRead();
            int remaining = cb.remaining();
            int frontSpace = start_;
            int required = inputSize - backSpace;
            if (frontSpace >= required) {
                cb.compact().flip();
                start_ = cb.position();
                end_ = cb.limit();
            } else {
                Chunk<ByteBuffer> newChunk = chunk_.reallocate(
                        Math.max(remaining * EXPAND_MULTIPLIER, remaining + inputSize));
                cb = newChunk.initialize();
                cb.put(buffer_).flip();
                chunk_ = newChunk;
                start_ = 0;
                end_ = remaining;
                buffer_ = cb;
            }
        }
        changeModeToWrite();
        cb.put(inputBuffer);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int remaining() {
        syncBeginEnd();
        return end_ - start_;
    }

    @Override
    public void dispose() {
        chunk_.release();
    }

    @Override
    public int space() {
        syncBeginEnd();
        return buffer_.capacity() - end_;
    }

    @Override
    public int capacity() {
        return buffer_.capacity();
    }

    @Override
    public int startIndex() {
        syncBeginEnd();
        return start_;
    }

    @Override
    public CodecBuffer startIndex(int start) {
        syncBeginEnd();
        if (mode_ == Mode.READ) {
            buffer_.position(start);
        } else {
            if (start < 0) {
                throw new IndexOutOfBoundsException("startIndex is negative.");
            }
            if (start > end_) {
                throw new IndexOutOfBoundsException("startIndex is greater than endIndex.");
            }
        }
        start_ = start;
        return this;
    }

    @Override
    public int endIndex() {
        syncBeginEnd();
        return end_;
    }

    @Override
    public CodecBuffer endIndex(int end) {
        syncBeginEnd();
        if (mode_ == Mode.WRITE) {
            buffer_.position(end);
        } else {
            buffer_.limit(end);
        }
        end_ = end;
        return this;
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
    public int drainFrom(CodecBuffer decodeBuffer, int bytes) {
        if (bytes < 0) {
            throw new IllegalArgumentException("bytes must be >= 0.");
        }
        int remaining = decodeBuffer.remaining();
        return drainFromNoCheck(decodeBuffer, (bytes <= remaining) ? bytes : remaining);
    }

    /**
     * Drains data from {@code input} with specified bytes.
     * @param input buffer which contains the data transferred from
     * @param bytes the number of byte to be transferred
     * @return {@code bytes}
     */
    private int drainFromNoCheck(CodecBuffer input, int bytes) {
        changeModeToWrite();
        ensureSpace(bytes);

        ByteBuffer bb = buffer_;
        int limit = bb.limit();
        bb.limit(bb.position() + bytes);
        input.readBytes(bb);
        bb.limit(limit);
        return bytes;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public CodecBuffer clear() {
        ByteBuffer b = buffer_;
        b.position(0);
        b.limit(0);
        start_ = 0;
        end_ = 0;
        mode_ = Mode.READ;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ByteBuffer byteBuffer() {
        changeModeToRead();
        return buffer_.duplicate();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasArray() {
        return buffer_.hasArray();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public byte[] array() {
        if (buffer_.hasArray()) {
            return buffer_.array();
        }
        int remaining = remaining();
        ByteBuffer bb = ByteBuffer.allocate(remaining);
        copyTo(bb);
        return bb.array();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int arrayOffset() {
        return buffer_.hasArray() ? buffer_.arrayOffset() : 0;
    }

    @Override
    public int indexOf(int b, int fromIndex) {
        changeModeToRead();
        if (fromIndex < 0) {
            fromIndex = 0;
        }
        int fromIndexInContent = fromIndex + start_;
        if (fromIndexInContent >= end_) {
            return -1;
        }

        int bb = (byte) b;
        int start = start_;
        ByteBuffer buffer = buffer_;
        buffer.position(fromIndexInContent);
        int count = buffer.limit() - fromIndexInContent;
        // relative get is faster than absolute get, maybe.
        for (int i = 0; i < count; i++) {
            if (buffer.get() == bb) {
                buffer.position(start);
                return fromIndex + i;
            }
        }
        buffer.position(start);
        return -1;
    }

    @Override
    public int indexOf(byte[] b, int fromIndex) {
        if (b == null || b.length == 0) {
            return -1;
        }

        changeModeToRead();
        if (fromIndex < 0) {
            fromIndex = 0;
        }
        int fromIndexInContent = fromIndex + start_;
        if (fromIndexInContent > end_ - b.length) {
            return -1;
        }

        int start = start_;
        ByteBuffer buffer = buffer_;
        buffer.position(fromIndexInContent);
        int count = buffer.limit() - fromIndexInContent - (b.length - 1);
        // relative get is faster than absolute get, maybe.
        BUFFER_LOOP: for (int i = 0; i < count; i++) {
            if (buffer.get() != b[0]) {
                continue;
            }
            for (int bi = 1; bi < b.length; bi++) {
                if (buffer.get() != b[bi]) {
                    buffer.position(fromIndexInContent + i);
                    continue BUFFER_LOOP;
                }
            }
            buffer.position(start);
            return fromIndex + i;
        }
        buffer.position(start_);
        return -1;
    }

    @Override
    public int lastIndexOf(int b, int fromIndex) {
        if (fromIndex < 0) {
            return -1;
        }

        changeModeToRead();
        int start = start_;
        int fromIndexInContent = fromIndex + start;
        if (fromIndexInContent >= end_) {
            fromIndexInContent = end_ - 1;
        }

        byte bb = (byte) b;
        ByteBuffer buffer = buffer_;
        for (int i = fromIndexInContent; i >= start; i--) {
            if (buffer.get(i) == bb) {
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

        changeModeToRead();
        int start = start_;
        int fromIndexInContent = fromIndex + start;
        if (fromIndexInContent >= end_ - b.length) {
            fromIndexInContent = end_ - b.length;
        }

        ByteBuffer buffer = buffer_;
        BUFFER_LOOP: for (int i = fromIndexInContent; i >= start; i--) {
            if (buffer.get(i) != b[0]) {
                continue;
            }
            for (int bi = 1; bi < b.length; bi++) {
                if (buffer.get(i + bi) != b[bi]) {
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
    public CodecBuffer slice(int bytes) {
        changeModeToRead();

        ByteBuffer bb = buffer_;
        if (bytes <= 0 || bytes > bb.remaining()) {
            throw new IllegalArgumentException("Invalid input " + bytes + ". " + bb.remaining() + " byte remains.");
        }
        CodecBuffer sliced = new SlicedCodecBuffer(duplicate(), bytes);
        bb.position(bb.position() + bytes);
        return sliced;
    }

    @Override
    public CodecBuffer slice() {
        return new SlicedCodecBuffer(duplicate());
    }

    @Override
    public CodecBuffer duplicate() {
        syncBeginEnd();
        return new ByteBufferCodecBuffer(this);
    }

    @Override
    public CodecBuffer compact() {
        changeModeToRead();
        if (start_ == 0) {
            return this;
        }

        ByteBuffer b = buffer_;
        b.compact().flip();
        start_ = b.position();
        end_ = b.limit();
        return this;
    }

    /**
     * Returns a summary of this buffer state.
     * @return a summary of this buffer state
     */
    @Override
    public String toString() {
        syncBeginEnd();
        return ByteBufferCodecBuffer.class.getName()
                + "(startIndex:" + start_ + ", endIndex:" + end_ + ", capacity:" + buffer_.capacity() + ')';
    }

    @Override
    public boolean equals(Object object) {
        if (object == this) {
            return true;
        }
        if (object instanceof ByteBufferCodecBuffer) {
            ByteBufferCodecBuffer that = (ByteBufferCodecBuffer) object;
            this.changeModeToRead();
            that.changeModeToRead();
            return this.buffer_.equals(that.buffer_);
        }
        if (object instanceof CodecBuffer) {
            changeModeToRead();
            CodecBuffer that = (CodecBuffer) object;
            return this.buffer_.equals(that.byteBuffer());
        }
        return false;
    }

    @Override
    public int hashCode() {
        changeModeToRead();
        return buffer_.hashCode();
    }

    /**
     * Returns a reference count of a chunk which manages an internal byte array.
     * @return the reference count
     */
    public int referenceCount() {
        return chunk_.referenceCount();
    }

    Chunk<ByteBuffer> chunk() {
        return chunk_;
    }
}
