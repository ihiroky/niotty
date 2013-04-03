package net.ihiroky.niotty.buffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.util.Objects;

/**
 * Implementation of {@link net.ihiroky.niotty.buffer.CodecBuffer} using {@code java.nio.ByteBuffer}.
 * @author Hiroki Itoh
 */
public class ByteBufferCodecBuffer extends AbstractCodecBuffer implements CodecBuffer {

    private ByteBuffer buffer_;
    private int beginning_;
    private int end_;
    private Mode mode_;

    private static final int EXPAND_MULTIPLIER = 2;
    private static final int DEFAULT_CAPACITY = 512;

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
        this(DEFAULT_CAPACITY);
    }

    ByteBufferCodecBuffer(int initialCapacity) {
        buffer_ = ByteBuffer.allocate(initialCapacity);
        mode_ = Mode.WRITE;
    }

    ByteBufferCodecBuffer(ByteBuffer buffer) {
        Objects.requireNonNull(buffer, "buffer");
        buffer_ = buffer;
        beginning_ = buffer_.position();
        end_ = buffer_.limit();
        mode_ = Mode.READ;
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
        ByteBuffer bb = buffer_;
        int current = bb.capacity();
        int required = bb.position() + space;
        if (required > current) {
            int twice = current * EXPAND_MULTIPLIER;
            ByteBuffer t = ByteBuffer.allocate((required >= twice) ? required : twice);
            bb.flip();
            t.put(bb);
            buffer_ = t;
        }
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
    public void writeByte(int position, int value) {
        if (position < 0 || position >= buffer_.capacity()) {
            throw new IndexOutOfBoundsException("position must be in [0, " + buffer_.capacity() + ")");
        }
        buffer_.put(position, (byte) value);
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
    public void writeString(CharsetEncoder charsetEncoder, String s) {
        if (s == null) {
            throw new NullPointerException("s must not be null.");
        }

        if (StringCache.writeEmptyOrOneCharAscii(this, charsetEncoder, s)) {
            return;
        }

        changeModeToWrite();
        int length = s.length();
        int expectedLengthBytes = CodecUtil.variableByteLength(
                Buffers.outputByteBufferSize(charsetEncoder.averageBytesPerChar(), length));
        ensureSpace(expectedLengthBytes);
        int startPosition = buffer_.position() + expectedLengthBytes;
        buffer_.position(startPosition);
        CharBuffer input = CharBuffer.wrap(s);
        ByteBuffer output = buffer_;
        for (;;) {
            CoderResult cr = charsetEncoder.encode(input, output, true);
            if (!cr.isError() && !cr.isOverflow()) {
                break;
            }
            if (cr.isOverflow()) {
                ensureSpace((int) (charsetEncoder.averageBytesPerChar() * input.remaining() + 1));
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
        int outputLength = buffer_.position() - startPosition;
        int lengthBytes = CodecUtil.variableByteLength(outputLength);
        if (lengthBytes != expectedLengthBytes) {
            int delta = lengthBytes - expectedLengthBytes;
            ensureSpace(delta);
            byte[] b = buffer_.array(); // TODO direct buffer operation
            System.arraycopy(b, buffer_.arrayOffset() + startPosition,
                    b, buffer_.arrayOffset() + startPosition + delta, outputLength);
            buffer_.position(buffer_.position() + delta);
        }
        int tmp = buffer_.position();
        buffer_.position(startPosition - expectedLengthBytes);
        writeVariableByteInteger(outputLength);
        buffer_.position(tmp);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int readByte(int position) {
        if (position < 0 || position >= buffer_.capacity()) {
            throw new IndexOutOfBoundsException("position must be in [0, " + buffer_.capacity() + ")");
        }
        return buffer_.get(position);
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
    public void readBytes(byte[] bytes, int offset, int length) {
        changeModeToRead();
        buffer_.get(bytes, offset, length);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readBytes(ByteBuffer byteBuffer) {
        changeModeToRead();
        ByteBuffer myBuffer = buffer_;
        int space = byteBuffer.remaining();
        if (space >= myBuffer.remaining()) {
            byteBuffer.put(myBuffer);
            return;
        }
        int limit = myBuffer.limit();
        myBuffer.limit(myBuffer.position() + space);
        byteBuffer.put(myBuffer);
        myBuffer.limit(limit);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int readBytes4(int bytes) {
        if (bytes < 0 || bytes > CodecUtil.INT_BYTES) {
            throw new IllegalArgumentException("bytes must be in [0, 4].");
        }

        changeModeToRead();
        int decoded = 0;
        for (int i = 0; i < bytes; i++) {
            decoded = (decoded << CodecUtil.BITS_PER_BYTE) | (buffer_.get() & CodecUtil.BYTE_MASK);
        }
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

        changeModeToRead();
        long decoded = 0L;
        for (int i = 0; i < bytes; i++) {
            decoded = (decoded << CodecUtil.BITS_PER_BYTE) | (buffer_.get() & CodecUtil.BYTE_MASK);
        }
        return decoded;
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
    public String readString(CharsetDecoder charsetDecoder, int bytes) {
        changeModeToRead();
        String cached = StringCache.getCachedValue(this, charsetDecoder, bytes);
        if (cached != null) {
            return cached;
        }

        float charsPerByte = charsetDecoder.maxCharsPerByte();
        ByteBuffer input = buffer_;
        CharBuffer output = CharBuffer.allocate(Buffers.outputCharBufferSize(charsPerByte, bytes));
        int limit = input.limit();
        input.limit(input.position() + bytes);
        for (;;) {
            CoderResult cr = charsetDecoder.decode(input, output, true);
            if (!cr.isError() && !cr.isOverflow()) {
                input.limit(limit);
                break;
            }
            if (cr.isOverflow()) {
                output = Buffers.expand(output, charsPerByte, input.remaining());
                continue;
            }
            if (cr.isError()) {
                input.limit(limit);
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
    public boolean transferTo(WritableByteChannel channel, ByteBuffer writeBuffer) throws IOException {
        changeModeToRead();

        ByteBuffer myBuffer = buffer_;
        if (myBuffer.isDirect()) {
            return transferDirectTo(channel, myBuffer);
        }
        int remaining = myBuffer.remaining();
        int limit = buffer_.limit();
        while (remaining > 0) {
            int space = writeBuffer.remaining();
            int readyToWrite = (remaining <= space) ? remaining : space;
            myBuffer.limit(myBuffer.position() + readyToWrite);
            writeBuffer.put(myBuffer);
            writeBuffer.flip();
            int writeBytes = channel.write(writeBuffer);
            // Some bytes remains in writeBuffer. Stop this round.
            if (writeBytes < readyToWrite) {
                myBuffer.limit(limit);
                myBuffer.position(limit - remaining + writeBytes);
                writeBuffer.clear();
                return false;
            }
            // Write all bytes in writeBuffer.
            remaining -= writeBytes;
            writeBuffer.clear();
        }
        clear();
        return true;
    }

    private boolean transferDirectTo(WritableByteChannel channel, ByteBuffer myBuffer) throws IOException {
        int remaining = myBuffer.remaining();
        if (remaining == 0) {
            return true;
        }
        int writeBytes = channel.write(myBuffer);
        // Some bytes remains in writeBuffer. Stop this round.
        if (writeBytes < remaining) {
            return false;
        }
        clear();
        return true;
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
    public int priority() {
        return Buffers.DEFAULT_PRIORITY;
    }

    @Override
    public void dispose() {
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
            if (end < beginning_) {
                throw new IndexOutOfBoundsException("end is less than beginning.");
            }
            if (end > buffer_.capacity()) {
                throw new IndexOutOfBoundsException("end is greater than capacity.");
            }
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
        buffer_.position(0);
        buffer_.limit(0);
        beginning_ = 0;
        end_ = 0;
        mode_ = Mode.READ;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ByteBuffer toByteBuffer() {
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
    public byte[] toArray() {
        return buffer_.array();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int arrayOffset() {
        return buffer_.arrayOffset();
    }

    /**
     * {@inheritDoc}
     */
    public CodecBuffer slice(int bytes) {
        changeModeToRead();

        ByteBuffer bb = buffer_;
        if (bytes < 0 || bytes > bb.remaining()) {
            throw new IllegalArgumentException("Invalid input " + bytes + ". " + bb.remaining() + " byte remains.");
        }
        ByteBuffer sliced = bb.slice();
        sliced.limit(bytes);
        bb.position(bb.position() + bytes);
        return new ByteBufferCodecBuffer(sliced);
    }

    @Override
    public CodecBuffer compact() {
        changeModeToRead();
        buffer_.compact();
        return this;
    }

    /**
     * Returns a summary of this buffer state.
     * @return a summary of this buffer state
     */
    @Override
    public String toString() {
        syncBeginEnd();
        return "(beginning:" + beginning_ + ", end:" + end_ + ", capacity:" + buffer_.capacity()
                + ", priority:" + priority() + ')';
    }
}
