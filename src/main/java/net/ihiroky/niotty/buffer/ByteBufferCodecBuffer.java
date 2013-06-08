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
 * Implementation of {@link net.ihiroky.niotty.buffer.CodecBuffer} using {@code java.nio.ByteBuffer}.
 * @author Hiroki Itoh
 */
public class ByteBufferCodecBuffer extends AbstractCodecBuffer {

    private Chunk<ByteBuffer> chunk_;
    private ByteBuffer buffer_;
    private int beginning_;
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

    ByteBufferCodecBuffer(ByteBuffer buffer) {
        Objects.requireNonNull(buffer, "buffer");
        ByteBufferChunk c = new ByteBufferChunk(buffer, ByteBufferChunkFactory.heap());
        c.ready();
        chunk_ = c;
        buffer_ = chunk_.initialize();
        beginning_ = buffer.position();
        end_ = buffer.limit();
        mode_ = Mode.READ;
    }

    private ByteBufferCodecBuffer(ByteBufferCodecBuffer b) {
        chunk_ = b.chunk_;
        buffer_ = b.chunk_.retain();
        beginning_ = b.beginning_;
        end_ = b.end_;
        mode_ = b.mode_;
    }

    private void changeModeToWrite() {
        if (mode_ == Mode.WRITE) {
            end_ = buffer_.position();
        } else {
            mode_ = Mode.WRITE;
            ByteBuffer b = buffer_;
            beginning_ = b.position();
            end_ = b.limit();
            b.limit(b.capacity());
            b.position(end_);
        }
    }

    private void changeModeToRead() {
        if (mode_ == Mode.READ) {
            beginning_ = buffer_.position();
        } else {
            mode_ = Mode.READ;
            ByteBuffer b = buffer_;
            end_ = b.position();
            b.position(beginning_);
            b.limit(end_);
        }
    }

    private void syncBeginEnd() {
        if (mode_ == Mode.WRITE) {
            end_ = buffer_.position();
        } else {
            beginning_ = buffer_.position();
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

        int remaining = end_ - beginning_;
        int minExpandBase = (beginning_ == 0) ? buffer_.capacity() : remaining;
        int newCapacity = Math.max(remaining + space, minExpandBase * EXPAND_MULTIPLIER);
        Chunk<ByteBuffer> newChunk = chunk_.reallocate(newCapacity);
        ByteBuffer newBuffer = newChunk.initialize();
        bb.position(beginning_).limit(end_);
        newBuffer.put(bb);
        beginning_ = 0;
        end_ = newBuffer.position();
        chunk_ = newChunk;
        buffer_ = newBuffer;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeByte(int value) {
        changeModeToWrite();
        ensureSpace(1);
        buffer_.put((byte) value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeBytes(byte[] bytes, int offset, int length) {
        changeModeToWrite();
        ensureSpace(length);
        buffer_.put(bytes, offset, length);
    }

    /**
     * {@inheritDoc}
     */
    public void writeBytes(ByteBuffer byteBuffer) {
        changeModeToWrite();
        ensureSpace(byteBuffer.remaining());
        buffer_.put(byteBuffer);
    }

    /**
     * Implementation of writeBytes8() without {@code byte} range check.
     * @param bits the set of bit to be written
     * @param bytes the byte size of {@code bits}
     */
    private void writeBytes8RangeUnchecked(long bits, int bytes) {
        changeModeToWrite();
        ensureSpace(bytes);
        int bytesMinus1 = bytes - 1;
        for (int i = 0; i < bytes; i++) {
            buffer_.put((byte) ((bits >>> (bytesMinus1 - i)) & CodecUtil.BYTE_MASK));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeShort(short value) {
        changeModeToWrite();
        ensureSpace(CodecUtil.SHORT_BYTES);
        buffer_.putShort(value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeChar(char value) {
        changeModeToWrite();
        ensureSpace(CodecUtil.CHAR_BYTES);
        buffer_.putChar(value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeInt(int value) {
        changeModeToWrite();
        ensureSpace(CodecUtil.INT_BYTES);
        buffer_.putInt(value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeLong(long value) {
        changeModeToWrite();
        ensureSpace(CodecUtil.LONG_BYTES);
        buffer_.putLong(value);
    }

    /**
     * {@inheritDoc}
     */
    public void writeString(String s, CharsetEncoder encoder) {
        Objects.requireNonNull(s, "s");
        Objects.requireNonNull(encoder, "encoder");

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
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int readByte() {
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

        float charsPerByte = decoder.averageCharsPerByte();
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
    public int skipBytes(int bytes) {
        changeModeToRead();
        ByteBuffer b = buffer_;
        int n = b.remaining();
        if (bytes < n) {
            n = (bytes < -b.position()) ? -b.position() : bytes;
        }
        b.position(b.position() + n);
        return n;
    }

    @Override
    public boolean transferTo(GatheringByteChannel channel) throws IOException {
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
    public void transferTo(ByteBuffer byteBuffer) {
        changeModeToRead();
        ByteBuffer myBuffer = buffer_;
        int position = myBuffer.position();
        int space = byteBuffer.remaining();
        int remaining = myBuffer.remaining();
        if (space >= remaining) {
            byteBuffer.put(myBuffer);
            myBuffer.position(position);
            return;
        }
        int limit = myBuffer.limit();
        myBuffer.limit(myBuffer.position() + space);
        byteBuffer.put(myBuffer);
        myBuffer.position(position);
        myBuffer.limit(limit);
    }

    @Override
    public ByteBufferCodecBuffer addFirst(CodecBuffer buffer) {
        Objects.requireNonNull(buffer, "buffer");
        int inputSize = buffer.remainingBytes();
        if (inputSize == 0) {
            return this;
        }

        changeModeToRead();
        ByteBuffer b = buffer_;
        ByteBuffer inputBuffer = buffer.byteBuffer();
        int position = b.position();

        int beginning;
        if (position >= inputSize) {
            beginning = position - inputBuffer.remaining();
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
            beginning_ = 0;
            beginning = 0;
        }

        b.position(beginning);
        b.put(inputBuffer);
        b.position(beginning);
        return this;
    }

    @Override
    public ByteBufferCodecBuffer addLast(CodecBuffer buffer) {
        Objects.requireNonNull(buffer, "buffer");
        int inputSize = buffer.remainingBytes();
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
            int frontSpace = beginning_;
            int required = inputSize - backSpace;
            if (frontSpace >= required) {
                cb.compact().flip();
                beginning_ = cb.position();
                end_ = cb.limit();
            } else {
                Chunk<ByteBuffer> newChunk = chunk_.reallocate(
                        Math.max(remaining * EXPAND_MULTIPLIER, remaining + inputSize));
                cb = newChunk.initialize();
                cb.put(buffer_).flip();
                chunk_ = newChunk;
                beginning_ = 0;
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
    public int remainingBytes() {
        syncBeginEnd();
        return end_ - beginning_;
    }

    @Override
    public void dispose() {
        chunk_.release();
    }

    @Override
    public int spaceBytes() {
        syncBeginEnd();
        return buffer_.capacity() - end_;
    }

    @Override
    public int capacityBytes() {
        return buffer_.capacity();
    }

    @Override
    public int beginning() {
        syncBeginEnd();
        return beginning_;
    }

    @Override
    public CodecBuffer beginning(int beginning) {
        syncBeginEnd();
        if (mode_ == Mode.READ) {
            buffer_.position(beginning);
        } else {
            if (beginning < 0) {
                throw new IndexOutOfBoundsException("beginning is negative.");
            }
            if (beginning > end_) {
                throw new IndexOutOfBoundsException("beginning is greater than end.");
            }
        }
        beginning_ = beginning;
        return this;
    }

    @Override
    public int end() {
        syncBeginEnd();
        return end_;
    }

    @Override
    public CodecBuffer end(int end) {
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
        return drainFromNoCheck(buffer, buffer.remainingBytes());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int drainFrom(CodecBuffer decodeBuffer, int bytes) {
        if (bytes < 0) {
            throw new IllegalArgumentException("bytes must be >= 0.");
        }
        int remaining = decodeBuffer.remainingBytes();
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
        beginning_ = 0;
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
        return buffer_.array();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int arrayOffset() {
        return buffer_.arrayOffset();
    }

    @Override
    public int indexOf(int b, int fromIndex) {
        changeModeToRead();
        if (fromIndex < 0) {
            fromIndex = 0;
        }
        int fromIndexInContent = fromIndex + beginning_;
        if (fromIndexInContent >= end_) {
            return -1;
        }

        int bb = (byte) b;
        int beginning = beginning_;
        ByteBuffer buffer = buffer_;
        buffer.position(fromIndexInContent);
        int count = buffer.limit() - fromIndexInContent;
        // relative get is faster than absolute get, maybe.
        for (int i = 0; i < count; i++) {
            if (buffer.get() == bb) {
                buffer.position(beginning);
                return fromIndex + i;
            }
        }
        buffer.position(beginning);
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
        int fromIndexInContent = fromIndex + beginning_;
        if (fromIndexInContent > end_ - b.length) {
            return -1;
        }

        int beginning = beginning_;
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
            buffer.position(beginning);
            return fromIndex + i;
        }
        buffer.position(beginning_);
        return -1;
    }

    @Override
    public int lastIndexOf(int b, int fromIndex) {
        if (fromIndex < 0) {
            return -1;
        }

        changeModeToRead();
        int beginning = beginning_;
        int fromIndexInContent = fromIndex + beginning;
        if (fromIndexInContent >= end_) {
            fromIndexInContent = end_ - 1;
        }

        byte bb = (byte) b;
        ByteBuffer buffer = buffer_;
        for (int i = fromIndexInContent; i >= beginning; i--) {
            if (buffer.get(i) == bb) {
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

        changeModeToRead();
        int beginning = beginning_;
        int fromIndexInContent = fromIndex + beginning;
        if (fromIndexInContent >= end_ - b.length) {
            fromIndexInContent = end_ - b.length;
        }

        ByteBuffer buffer = buffer_;
        BUFFER_LOOP: for (int i = fromIndexInContent; i >= beginning; i--) {
            if (buffer.get(i) != b[0]) {
                continue;
            }
            for (int bi = 1; bi < b.length; bi++) {
                if (buffer.get(i + bi) != b[bi]) {
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
        if (beginning_ == 0) {
            return this;
        }

        ByteBuffer b = buffer_;
        b.compact().flip();
        beginning_ = b.position();
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
                + "(beginning:" + beginning_ + ", end:" + end_ + ", capacity:" + buffer_.capacity()
                + ", attachment:" + ')';
    }

    Chunk<ByteBuffer> chunk() {
        return chunk_;
    }
}
