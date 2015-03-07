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
import java.util.ArrayList;
import java.util.List;

/**
 * A {@link CodecBuffer} which consists of {@link CodecBuffer}s.
 * <p></p>
 * The {@link #addFirst(CodecBuffer)} and {@link #addLast(CodecBuffer)} add the argument into an internal list.
 * The elements contained in the list is limited on its size when added to. So an expansion of each elements
 * does not happen. But the startIndex and endIndex of the added instance can change according to read and write operation
 * ot this instance. This object allocates a new heap {@code CodecBuffer} and add it to the list
 * if the object need expand its space. The maximum elements that can be held by the object is 1024.
 */
public class CodecBufferList extends AbstractCodecBuffer {

    private List<CodecBuffer> buffers_;
    private int startBufferIndex_;
    private int endBufferIndex_;

    private static final int INITIAL_BUFFERS_CAPACITY = 4;
    private static final int MAX_BUFFER_COUNT = 1024;

    private CodecBufferList() {
        buffers_ = new ArrayList<CodecBuffer>(INITIAL_BUFFERS_CAPACITY);
        endBufferIndex_ = -1;
    }

    CodecBufferList(CodecBuffer buffer0) {
        Arguments.requireNonNull(buffer0, "buffer0");

        List<CodecBuffer> list = new ArrayList<CodecBuffer>(INITIAL_BUFFERS_CAPACITY);
        list.add(new SlicedCodecBuffer(buffer0));
        buffers_ = list;
        endBufferIndex_ = 0;
    }

    CodecBufferList(CodecBuffer buffer0, CodecBuffer buffer1) {
        Arguments.requireNonNull(buffer0, "buffer0");
        Arguments.requireNonNull(buffer1, "buffer1");

        List<CodecBuffer> list = new ArrayList<CodecBuffer>(INITIAL_BUFFERS_CAPACITY);
        list.add(new SlicedCodecBuffer(buffer0));
        list.add(new SlicedCodecBuffer(buffer1));
        buffers_ = list;
        endBufferIndex_ = 1;
    }

    CodecBufferList(CodecBuffer... buffers) {
        Arguments.requireNonNull(buffers, "buffers");
        if (buffers.length >= MAX_BUFFER_COUNT) {
            throw new IllegalArgumentException("length of buffers must be less than " + MAX_BUFFER_COUNT + ".");
        }

        List<CodecBuffer> list = new ArrayList<CodecBuffer>(Math.max(INITIAL_BUFFERS_CAPACITY, buffers.length));
        int end = -1;
        for (CodecBuffer b : buffers) {
            list.add(new SlicedCodecBuffer(b));
            end++;
        }
        buffers_ = list;
        endBufferIndex_ = end;
    }

    int startBufferIndex() {
        return startBufferIndex_;
    }

    int endBufferIndex() {
        return endBufferIndex_;
    }

    int sizeOfBuffers() {
        return buffers_.size();
    }

    @Override
    public boolean sink(GatheringByteChannel channel) throws IOException {
        List<CodecBuffer> buffers = buffers_;
        int offset = startBufferIndex_;
        final int end = endBufferIndex_;
        for (; offset < end; offset++) {
            if (buffers.get(offset).remaining() > 0) {
                break;
            }
        }
        if (offset == end && buffers.get(offset).remaining() == 0) {
            return true;
        }
        ByteBuffer[] byteBuffers = new ByteBuffer[end - offset + 1];
        for (int i = offset; i <= end; i++) {
            byteBuffers[i - offset] = buffers.get(i).byteBuffer();
        }
        channel.write(byteBuffers, 0, byteBuffers.length);
        for (int i = offset; i <= end; i++) {
            ByteBuffer byteBuffer = byteBuffers[i - offset];
            buffers.get(i).startIndex(byteBuffer.position());
            if (byteBuffer.hasRemaining()) {
                startBufferIndex_ = i;
                return false;
            }
        }
        startBufferIndex_ = end;
        return true;
    }

    @Override
    public boolean sink(DatagramChannel channel, ByteBuffer buffer, SocketAddress target) throws IOException {
        List<CodecBuffer> buffers = buffers_;
        if (buffers.size() == 1) {
            return buffers.get(0).sink(channel, buffer, target);
        } else {
            for (CodecBuffer b : buffers) {
                b.copyTo(buffer);
            }
            buffer.flip();
            boolean sent = (channel.send(buffer, target) > 0);
            buffer.clear();
            return sent;
        }
    }

    @Override
    public void copyTo(ByteBuffer buffer) {
        int end = endBufferIndex_;
        List<CodecBuffer> buffers = buffers_;
        for (int i = startBufferIndex_; i <= end; i++) {
            buffers.get(i).copyTo(buffer);
        }
    }

    @Override
    public CodecBufferList addFirst(CodecBuffer buffer) {
        Arguments.requireNonNull(buffer, "buffer");

        int start = startBufferIndex_;
        while (start <= endBufferIndex_
                && buffers_.get(start).remaining() == 0) {
            start = ++startBufferIndex_;
        }

        buffers_.add(start, new SlicedCodecBuffer(buffer)); // wrap, not duplicated
        endBufferIndex_++;
        return this;
    }

    @Override
    public CodecBufferList addLast(CodecBuffer buffer) {
        if (buffer == null) {
            throw new IllegalArgumentException("buffer must not be null.");
        }

        int end = endBufferIndex_;
        while (end >= startBufferIndex_
                && buffers_.get(end).remaining() == 0) {
            end--;
        }

        // wrap, not duplicated
        int size = buffers_.size();
        if (end == size - 1) { // endIndex is the last index ?
            buffers_.add(new SlicedCodecBuffer(buffer));
            endBufferIndex_++;
        } else if (end >= 0) {
            buffers_.add(++end, new SlicedCodecBuffer(buffer));
            endBufferIndex_ = end;
        } else { // if (endIndex == -1) { // all buffer between startIndex and endIndex are empty.
            buffers_.add(new SlicedCodecBuffer(buffer));
            endBufferIndex_ = (buffer.remaining() > 0) ? size : startBufferIndex_;
        }
        return this;
    }

    private CodecBuffer appendNewCodecBuffer(CodecBuffer endBuffer, int expectedMinSize) {
        if (buffers_.size() >= MAX_BUFFER_COUNT) {
            throw new IllegalStateException("the size of buffers reaches maximum: " + MAX_BUFFER_COUNT);
        }
        CodecBuffer buffer = Buffers.newCodecBuffer(
                Math.max(endBuffer.capacity() * EXPAND_MULTIPLIER, expectedMinSize));

        buffers_.add(buffer);
        endBufferIndex_++;
        return buffer;
    }

    private CodecBuffer nextBuffer(CodecBuffer current, int required) {
        return (endBufferIndex_ + 1 == buffers_.size())
                ? appendNewCodecBuffer(current, required) : buffers_.get(++endBufferIndex_);
    }

    @Override
    public CodecBufferList writeByte(int value) {
        CodecBuffer buffer = buffers_.get(endBufferIndex_);
        if (buffer.space() == 0) {
            buffer = appendNewCodecBuffer(buffer, 1);
        }
        buffer.writeByte(value);
        return this;
    }

    @Override
    public CodecBufferList writeBytes(byte[] bytes, int offset, int length) {
        CodecBuffer buffer = buffers_.get(endBufferIndex_);
        int space = buffer.space();
        if (space >= length) {
            buffer.writeBytes(bytes, offset, length);
            return this;
        }
        buffer.writeBytes(bytes, offset, space);

        length -= space;
        CodecBuffer newBuffer = appendNewCodecBuffer(buffer, length);
        newBuffer.writeBytes(bytes, offset + space, length);
        return this;
    }

    @Override
    public CodecBufferList writeBytes(ByteBuffer byteBuffer) {
        CodecBuffer buffer = buffers_.get(endBufferIndex_);
        int space = buffer.space();
        if (space >= byteBuffer.remaining()) {
            buffer.writeBytes(byteBuffer);
            return this;
        }
        int limit = byteBuffer.limit();
        byteBuffer.limit(byteBuffer.position() + space);
        buffer.writeBytes(byteBuffer);
        byteBuffer.limit(limit);

        int writeBytesInNewBuffer = byteBuffer.remaining();
        CodecBuffer newBuffer = appendNewCodecBuffer(buffer, writeBytesInNewBuffer);
        newBuffer.writeBytes(byteBuffer);
        return this;
    }

    @Override
    public CodecBufferList writeShort(int value) {
        CodecBuffer buffer = buffers_.get(endBufferIndex_);
        if (buffer.space() >= CodecUtil.SHORT_BYTES) {
            buffer.writeShort(value);
            return this;
        }
        CodecBuffer nextBuffer = nextBuffer(buffer, CodecUtil.SHORT_BYTES);
        nextBuffer.writeShort(value);
        return this;
    }

    @Override
    public CodecBufferList writeChar(char value) {
        CodecBuffer buffer = buffers_.get(endBufferIndex_);
        if (buffer.space() >= CodecUtil.CHAR_BYTES) {
            buffer.writeChar(value);
            return this;
        }
        CodecBuffer newBuffer = nextBuffer(buffer, CodecUtil.CHAR_BYTES);
        newBuffer.writeChar(value);
        return this;
    }

    @Override
    public CodecBufferList writeMedium(int value) {
        CodecBuffer buffer = buffers_.get(endBufferIndex_);
        if (buffer.space() >= CodecUtil.MEDIUM_BYTES) {
            buffer.writeMedium(value);
            return this;
        }
        writeShort((value >>> CodecUtil.BYTE_SHIFT1) & CodecUtil.SHORT_MASK);
        writeByte(value & CodecUtil.BYTE_MASK);
        return this;
    }

    @Override
    public CodecBufferList writeInt(int value) {
        CodecBuffer buffer = buffers_.get(endBufferIndex_);
        if (buffer.space() >= CodecUtil.INT_BYTES) {
            buffer.writeInt(value);
            return this;
        }
        writeShort((value >>> CodecUtil.BYTE_SHIFT2));
        writeShort((value & CodecUtil.SHORT_MASK));
        return this;
    }

    @Override
    public CodecBufferList writeLong(long value) {
        CodecBuffer buffer = buffers_.get(endBufferIndex_);
        if (buffer.space() >= CodecUtil.LONG_BYTES) {
            buffer.writeLong(value);
            return this;
        }
        writeInt((int) (value >>> CodecUtil.BYTE_SHIFT4));
        writeInt((int) (value & CodecUtil.INT_MASK));
        return this;
    }

    @Override
    public CodecBufferList writeString(String s, CharsetEncoder encoder) {
        Arguments.requireNonNull(s, "s");
        Arguments.requireNonNull(encoder, "encoder");

        CodecBuffer buffer = buffers_.get(endBufferIndex_);
        CharBuffer input = CharBuffer.wrap(s);
        ByteBuffer output = buffer.byteBuffer();
        output.position(output.limit());
        output.limit(output.capacity());
        for (;;) {
            CoderResult cr = encoder.encode(input, output, true);
            if (cr.isUnderflow()) {
                cr = encoder.flush(output);
                if (cr.isUnderflow()) {
                    buffer.endIndex(output.position());
                    encoder.reset();
                    break;
                }
                // jump ot overflow operation.
            }
            if (cr.isOverflow()) {
                buffer.endIndex(output.position());
                if (endBufferIndex_ + 1 == buffers_.size()) {
                    buffer = appendNewCodecBuffer(
                            buffer, Buffers.outputByteBufferSize(encoder, input.remaining()));
                } else {
                    buffer = buffers_.get(++endBufferIndex_);
                }
                output = buffer.byteBuffer();
                output.position(output.limit());
                output.limit(output.capacity());
                continue;
            }
            if (cr.isError()) {
                buffer.endIndex(output.position());
                try {
                    cr.throwException();
                } catch (CharacterCodingException cce) {
                    throw new RuntimeException(cce);
                }
            }
        }
        return this;
    }

    private CodecBuffer nextReadBufferOrNull() {
        CodecBuffer buffer = buffers_.get(startBufferIndex_);
        while (buffer.remaining() == 0) {
            if (startBufferIndex_ >= endBufferIndex_) {
                return null;
            }
            buffer = buffers_.get(++startBufferIndex_);
        }
        return buffer;
    }

    private CodecBuffer nextReadBuffer() {
        CodecBuffer buffer = buffers_.get(startBufferIndex_);
        while (buffer.remaining() == 0) {
            if (startBufferIndex_ >= endBufferIndex_) {
                throw new IndexOutOfBoundsException("No data remains.");
            }
            buffer = buffers_.get(++startBufferIndex_);
        }
        return buffer;
    }

    @Override
    public byte readByte() {
        CodecBuffer buffer = nextReadBuffer();
        return buffer.readByte();
    }

    @Override
    public int readUnsignedByte() {
        CodecBuffer buffer = nextReadBuffer();
        return buffer.readUnsignedByte();
    }

    @Override
    public int readBytes(byte[] bytes, int offset, int length) {
        int space = length;
        while (space > 0) {
            CodecBuffer buffer = nextReadBufferOrNull();
            if (buffer == null) {
                break;
            }
            int remaining = buffer.remaining();
            if (remaining > 0) {
                int read = buffer.readBytes(bytes, offset, space);
                offset += read;
                space -= read;
            }
        }
        return length - space;
    }

    @Override
    public int readBytes(ByteBuffer byteBuffer) {
        int readTotal = 0;
        while (byteBuffer.remaining() > 0) {
            CodecBuffer buffer = nextReadBufferOrNull();
            if (buffer == null) {
                break;
            }
            int remaining = buffer.remaining();
            if (remaining > 0) {
                readTotal += buffer.readBytes(byteBuffer);
            }
        }
        return readTotal;
    }

    @Override
    public char readChar() {
        CodecBuffer buffer = nextReadBuffer();
        if (buffer.remaining() >= CodecUtil.CHAR_BYTES) {
            return buffer.readChar();
        }
        return (char) ((readUnsignedByte() << CodecUtil.BYTE_SHIFT1) | readUnsignedByte());
    }

    @Override
    public short readShort() {
        CodecBuffer buffer = nextReadBuffer();
        if (buffer.remaining() >= CodecUtil.SHORT_BYTES) {
            return buffer.readShort();
        }
        return (short) ((readUnsignedByte() << CodecUtil.BYTE_SHIFT1) | readUnsignedByte());
    }

    @Override
    public int readUnsignedMedium() {
        CodecBuffer buffer = nextReadBuffer();
        if (buffer.remaining() >= CodecUtil.MEDIUM_BYTES) {
            return buffer.readUnsignedMedium();
        }
        return ((readShort() & CodecUtil.SHORT_MASK) << CodecUtil.BYTE_SHIFT1) | readUnsignedByte();
    }

    @Override
    public int readInt() {
        CodecBuffer buffer = nextReadBuffer();
        if (buffer.remaining() >= CodecUtil.INT_BYTES) {
            return buffer.readInt();
        }
        return ((readShort() & CodecUtil.SHORT_MASK) << CodecUtil.BYTE_SHIFT2) | (readShort() & CodecUtil.SHORT_MASK);
    }

    @Override
    public long readLong() {
        CodecBuffer buffer = nextReadBuffer();
        if (buffer.remaining() >= CodecUtil.LONG_BYTES) {
            return buffer.readLong();
        }
        return ((readInt() & CodecUtil.INT_MASK) << CodecUtil.BYTE_SHIFT4) | (readInt() & CodecUtil.INT_MASK);
    }

    @Override
    public float readFloat() {
        return Float.intBitsToFloat(readInt());
    }

    @Override
    public double readDouble() {
        return Double.longBitsToDouble(readLong());
    }

    @Override
    public String readString(CharsetDecoder decoder, int length) {
        String cached = StringCache.getCachedValue(this, decoder, length);
        if (cached != null) {
            return cached;
        }

        CodecBuffer buffer = nextReadBuffer();
        ByteBuffer input = buffer.byteBuffer();
        CharBuffer output = CharBuffer.allocate(Buffers.outputCharBufferSize(decoder, length));
        int currentRemaining = input.remaining();
        boolean endOfInput = currentRemaining >= length;
        if (endOfInput) {
            input.limit(input.position() + length);
        }
        int previousRemaining = 0;
        for (;;) {
            CoderResult cr = decoder.decode(input, output, endOfInput);
            if (cr.isUnderflow()) {
                if (endOfInput) {
                    cr = decoder.flush(output);
                    if (cr.isUnderflow()) {
                        buffer.startIndex(input.position() - previousRemaining);
                        decoder.reset();
                        break;
                    }
                    // jump to overflow operation.
                } else {
                    final int remaining = input.remaining();
                    previousRemaining = remaining;
                    buffer.startIndex(buffer.endIndex());
                    buffer = nextReadBuffer();
                    if (remaining == 0) {
                        input = buffer.byteBuffer();
                    } else {
                        ByteBuffer newInput = ByteBuffer.allocate(remaining + buffer.remaining());
                        newInput.put(input).put(buffer.byteBuffer()).flip();
                        input = newInput;
                    }
                    length -= currentRemaining - remaining;
                    currentRemaining = input.remaining();
                    endOfInput = currentRemaining >= length;
                    if (endOfInput) {
                        input.limit(input.position() + length);
                    }
                    continue;
                }
            }
            if (cr.isOverflow()) {
                output = Buffers.expand(output, decoder, input.remaining());
                continue;
            }
            if (cr.isError()) {
                buffer.startIndex(input.position() - previousRemaining);
                Buffers.throwRuntimeException(cr);
            }
        }
        output.flip();
        return StringCache.toString(output, decoder);
    }

    @Override
    public int skipStartIndex(int n) {
        int rest = n;
        int bufferIndex = startBufferIndex_;
        List<CodecBuffer> buffers = buffers_;
        for (;;) {
            CodecBuffer buffer = buffers.get(bufferIndex);
            rest -= buffer.skipStartIndex(rest);
            if (n >= 0) {
                if (buffer.remaining() > 0 || bufferIndex == endBufferIndex_) {
                    break;
                }
                bufferIndex++;
            } else {
                if (rest == 0 || bufferIndex == 0) {
                    break;
                }
                bufferIndex--;
            }
        }
        startBufferIndex_ = bufferIndex;
        return n - rest;
    }

    @Override
    public int skipEndIndex(int n) {
        int rest = n;
        int bufferIndex = endBufferIndex_;
        List<CodecBuffer> buffers = buffers_;
        int lastBufferIndex = buffers.size() - 1;
        for (;;) {
            CodecBuffer buffer = buffers.get(bufferIndex);
            rest -= buffer.skipEndIndex(rest);
            if (n >= 0) {
                if (buffer.space() > 0 || bufferIndex == lastBufferIndex) {
                    break;
                }
                bufferIndex++;
            } else {
                if (rest == 0 || bufferIndex == startBufferIndex_) {
                    break;
                }
                bufferIndex--;
            }
        }
        endBufferIndex_ = bufferIndex;
        return n - rest;
    }

    static int toNonNegativeInt(long value) {
        return (value <= Integer.MAX_VALUE) ? (int) value : Integer.MAX_VALUE;
    }

    @Override
    public int remaining() {
        return toNonNegativeInt(remainingBytesLong());
    }

    /**
     * Returns the size of remaining data by the byte as long type.
     * @return the size of remaining data by the byte as long type.
     */
    public long remainingBytesLong() {
        long sum = 0;
        List<CodecBuffer> buffers = buffers_;
        int end = endBufferIndex_;
        for (int i = startBufferIndex_; i <= end; i++) {
            sum += buffers.get(i).remaining();
        }
        return sum;
    }

    @Override
    public int space() {
        return toNonNegativeInt(spaceBytesLong());
    }

    /**
     * Returns the size of space to be written data by the byte as long type.
     * @return the size of space to be written data by the byte as long type.
     */
    public long spaceBytesLong() {
        long sum = 0;
        int size = buffers_.size();
        for (int i = endBufferIndex_; i < size; i++) {
            sum += buffers_.get(i).space();
        }
        return sum;
    }

    @Override
    public int capacity() {
        return toNonNegativeInt(capacityBytesLong());
    }

    /**
     * Returns the capacity of this buffer by the byte as long type.
     * @return the capacity of this buffer by the byte as long type.
     */
    public long capacityBytesLong() {
        long sum = 0;
        for (CodecBuffer buffer : buffers_) {
            sum += buffer.capacity();
        }
        return sum;
    }

    @Override
    public int startIndex() {
        return toNonNegativeInt(startIndexLong());
    }

    /**
     * Returns the value of the startIndex as long type.
     * @return the value of the startIndex as long type.
     */
    public long startIndexLong() {
        List<CodecBuffer> buffer = buffers_;
        int startBufferIndex = startBufferIndex_;
        long start = 0L;
        for (int i = 0; i < startBufferIndex; i++) {
            start += buffer.get(i).capacity();
        }
        start += buffer.get(startBufferIndex).startIndex();
        return start;
    }

    @Override
    public CodecBuffer startIndex(int start) {
        return startIndexLong(start);
    }

    /**
     * Sets the value of the startIndex as long type.
     *
     * @param start the value to be set
     * @return this {@code CodecBuffer}
     * @throws java.lang.IndexOutOfBoundsException if {@code startIndex} is out of range
     */
    public CodecBuffer startIndexLong(long start) {
        if (start < 0) {
            throw new IndexOutOfBoundsException("startIndex must be more than 0.");
        }
        List<CodecBuffer> buffer = buffers_;
        int endBufferIndex = endBufferIndex_;
        CodecBuffer target = null;
        for (int bi = startBufferIndex_; bi <= endBufferIndex; bi++) {
            CodecBuffer b = buffer.get(bi);
            int c = b.capacity();
            if (start < c) {
                target = b;
                break;
            }
            start -= c;
        }
        if (target == null) {
            throw new IndexOutOfBoundsException("startIndex is greater than endIndex.");
        }
        target.startIndex((int) start);
        return this;
    }

    @Override
    public int endIndex() {
        return toNonNegativeInt(endLong());
    }

    /**
     * Returns the value of the endIndex as long type.
     * @return the value of the endIndex as long type.
     */
    public long endLong() {
        long sum = 0;
        int end = endBufferIndex_;
        for (int i = startBufferIndex_; i < end; i++) {
            sum += buffers_.get(i).capacity();
        }
        sum += buffers_.get(end).endIndex();
        return sum;
    }

    @Override
    public CodecBuffer endIndex(int end) {
        return endLong(end);
    }

    /**
     * Sets the value of the endIndex as long type.
     *
     * @param end the value to be set
     * @return this {@code CodecBuffer}
     * @throws java.lang.IndexOutOfBoundsException if {@code endIndex} is out of range
     */
    public CodecBuffer endLong(long end) {
        List<CodecBuffer> buffers = buffers_;
        int size = buffers.size();
        CodecBuffer target = null;
        long index = 0;
        for (int bi = 0; bi < size; bi++) {
            target = buffers.get(bi);
            if (end < index) {
                break;
            }
            index += target.capacity();
        }
        if (index < end) {
            throw new IndexOutOfBoundsException("endIndex must be in the buffer: " + end);
        }
        if (target != null) {
            target.endIndex((int) end);
        }
        return this;
    }

    @Override
    public int drainFrom(CodecBuffer buffer) {
        return drainFrom(buffer, buffer.remaining());
    }

    @Override
    public int drainFrom(CodecBuffer buffer, int bytes) {
        // TODO over int operation
        int drainedBytes = 0;
        CodecBuffer b = null;
        List<CodecBuffer> buffers = buffers_;
        int size = buffers.size();
        int end = endBufferIndex_;
        for (; end < size; end++) {
            b = buffers.get(end);
            int space = b.space();
            if (space >= (bytes - drainedBytes)) {
                endBufferIndex_ = end;
                return drainedBytes + b.drainFrom(buffer, (bytes - drainedBytes));
            }
            drainedBytes += b.drainFrom(buffer, space);
        }
        if (b != null) {
            endBufferIndex_ = end - 1;
        } else {
            b = buffers.get(endBufferIndex_);
        }
        int writeInNewBuffer = bytes - drainedBytes;
        CodecBuffer newBuffer = appendNewCodecBuffer(b, writeInNewBuffer);
        newBuffer.drainFrom(buffer, writeInNewBuffer);
        return bytes; // read all
    }

    @Override
    public CodecBuffer slice(int bytes) {
        int wholeRemaining = remaining();
        if (bytes <= 0 || bytes > wholeRemaining) {
            throw new IllegalArgumentException("Invalid input " + bytes + ". " + wholeRemaining + " byte remains.");
        }

        CodecBuffer buffer = nextReadBuffer();
        int remaining = buffer.remaining();
        if (remaining >= bytes) {
            return buffer.slice(bytes);
        }
        CodecBufferList ccb = new CodecBufferList();
        CodecBuffer sliced = buffer.slice(remaining);
        bytes -= remaining;
        ccb.addLast(sliced);
        while (bytes > 0) {
            buffer = nextReadBuffer();
            remaining = buffer.remaining();
            if (remaining >= bytes) {
                sliced = buffer.slice(bytes);
                ccb.addLast(sliced);
                break;
            }
            sliced = buffer.slice(remaining);
            ccb.addLast(sliced);
            bytes -= remaining;
        }
        return ccb;
    }

    @Override
    public CodecBuffer slice() {
        CodecBufferList sliced = new CodecBufferList();
        for (int i = startBufferIndex_; i <= endBufferIndex_; i++) {
            sliced.addLast(buffers_.get(i));
        }
        return sliced;
    }

    @Override
    public CodecBuffer duplicate() {
        CodecBufferList duplicated = new CodecBufferList();
        duplicated.startBufferIndex_ = startBufferIndex_;
        duplicated.endBufferIndex_ = endBufferIndex_;
        for (CodecBuffer b : buffers_) {
            duplicated.buffers_.add(b.duplicate());
        }
        return duplicated;
    }

    @Override
    public CodecBuffer compact() {
        if (endBufferIndex_ < 0) {
            return this;
        }

        if (startBufferIndex_ > 0) {
            List<CodecBuffer> t = new ArrayList<CodecBuffer>(endBufferIndex_ - startBufferIndex_ + 1);
            for (int i = startBufferIndex_; i <= endBufferIndex_; i++) {
                t.add(buffers_.get(i));
            }
            buffers_ = t;
        }

        CodecBuffer first = buffers_.get(0);
        if (first.startIndex() == 0) {
            return this;
        }
        first.compact();
        return this;
    }

    @Override
    public CodecBuffer clear() {
        for (CodecBuffer b : buffers_) {
            b.clear();
        }
        startBufferIndex_ = 0;
        endBufferIndex_ = 0;
        return this;
    }

    @Override
    public ByteBuffer byteBuffer() {
        int begin = startBufferIndex_;
        int end = endBufferIndex_;
        if (begin == end) {
            return buffers_.get(end).byteBuffer();
        }
        long remaining = remainingBytesLong();
        if (remaining > Integer.MAX_VALUE) {
            throw new IllegalStateException("remaining is greater than Integer.MAX_VALUE.");
        }
        ByteBuffer byteBuffer = ByteBuffer.allocate((int) remaining);
        for (int i = begin; i <= end; i++) {
            CodecBuffer buffer = buffers_.get(i);
            byteBuffer.put(buffer.byteBuffer());
        }
        byteBuffer.flip();
        return byteBuffer;
    }

    @Override
    public boolean hasArray() {
        if (startBufferIndex_ == endBufferIndex_) {
            CodecBuffer buffer = buffers_.get(endBufferIndex_);
            if (buffer.hasArray()) {
                return buffer.hasArray();
            }
        }
        return false;
    }

    @Override
    public byte[] array() {
        if (startBufferIndex_ == endBufferIndex_) {
            CodecBuffer buffer = buffers_.get(endBufferIndex_);
            if (buffer.hasArray()) {
                return buffer.array();
            }
        }
        int remaining = remaining();
        ByteBuffer bb = ByteBuffer.allocate(remaining);
        copyTo(bb);
        return bb.array();
    }

    @Override
    public int arrayOffset() {
        if (startBufferIndex_ == endBufferIndex_) {
            CodecBuffer buffer = buffers_.get(endBufferIndex_);
            if (buffer.hasArray()) {
                return buffer.arrayOffset();
            }
        }
        return 0;
    }

    @Override
    public int indexOf(int b, int fromIndex) {
        if (fromIndex < 0) {
            fromIndex = 0;
        }

        List<CodecBuffer> buffers = buffers_;
        int begin = startBufferIndex_;
        int end = endBufferIndex_;
        long offset = 0;

        CodecBuffer buffer = null;
        while (begin <= end) {
            CodecBuffer cb = buffers.get(begin++);
            int remaining = cb.remaining();
            if (remaining + offset >= fromIndex) {
                buffer = cb;
                break;
            }
            offset += remaining;
        }
        if (buffer == null) {
            return -1;
        }
        int index = buffer.indexOf(b, (int) (fromIndex - offset));
        if (index != -1) {
            return toNonNegativeInt(offset + index); // TODO return long
        }
        offset += buffer.remaining();
        for (; begin < end; begin++) {
            buffer = buffers.get(begin);
            index = buffer.indexOf(b, 0);
            if (index != -1) {
                return toNonNegativeInt(offset + index);
            }
            offset += buffer.remaining();
        }
        return -1;
    }

    @Override
    public int indexOf(byte[] b, int fromIndex) {
        if (fromIndex < 0) {
            fromIndex = 0;
        }

        List<CodecBuffer> buffers = buffers_;
        int begin = startBufferIndex_;
        int end = endBufferIndex_;
        long offset = 0;

        CodecBuffer buffer = null;
        while (begin <= end) {
            CodecBuffer cb = buffers.get(begin++);
            int remaining = cb.remaining();
            if (remaining + offset >= fromIndex) {
                buffer = cb;
                break;
            }
            if (begin > end) {
                return -1;
            }
            offset += remaining;
        }
        if (buffer == null) {
            return -1;
        }
        int index = buffer.indexOf(b, (int) (fromIndex - offset));
        if (index != -1) {
            return toNonNegativeInt(offset + index);
        }
        offset += buffer.remaining();
        for (; begin < end; begin++) {
            buffer = buffers.get(begin);
            index = buffer.indexOf(b, 0);
            if (index != -1) {
                return toNonNegativeInt(offset + index);
            }
            offset += buffer.remaining();
        }
        return -1;
    }

    @Override
    public int lastIndexOf(int b, int fromIndex) {
        List<CodecBuffer> buffers = buffers_;
        int begin = startBufferIndex_;
        int end = endBufferIndex_;
        int offset = remaining(); // TODO long

        if (fromIndex >= offset) {
            fromIndex = offset;
        }
        CodecBuffer buffer;
        for (;;) {
            buffer = buffers.get(end--);
            offset -= buffer.remaining();
            if (offset <= fromIndex) {
                break;
            }
            if (end < begin) {
                return -1;
            }
        }
        int index = buffer.lastIndexOf(b, fromIndex - offset);
        if (index != -1) {
            return toNonNegativeInt(offset + index);
        }
        offset += buffer.remaining();
        for (; end >= begin; end--) {
            buffer = buffers.get(begin);
            index = buffer.indexOf(b, 0);
            if (index != -1) {
                return offset + index;
            }
            offset += buffer.remaining();
        }
        return -1;
    }

    @Override
    public int lastIndexOf(byte[] b, int fromIndex) {
        List<CodecBuffer> buffers = buffers_;
        int begin = startBufferIndex_;
        int end = endBufferIndex_;
        int offset = remaining(); // TODO long

        CodecBuffer buffer;
        if (fromIndex >= offset) {
            fromIndex = offset;
        }
        for (;;) {
            buffer = buffers.get(end--);
            offset -= buffer.remaining();
            if (offset <= fromIndex) {
                break;
            }
            if (end < begin) {
                return -1;
            }
        }
        int index = buffer.lastIndexOf(b, fromIndex - offset);
        if (index != -1) {
            return toNonNegativeInt(offset + index);
        }
        offset += buffer.remaining();
        for (; end >= begin; end--) {
            buffer = buffers.get(begin);
            index = buffer.indexOf(b, 0);
            if (index != -1) {
                return offset + index;
            }
            offset += buffer.remaining();
        }
        return -1;
    }

    @Override
    public void dispose() {
        for (CodecBuffer buffer : buffers_) {
            buffer.dispose();
        }
        buffers_.clear();
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append('[');
        int index = startBufferIndex_;
        int end = endBufferIndex_;
        List<CodecBuffer> buffers = buffers_;
        if (index <= end) {
            b.append(buffers.get(index));
            while (++index <= end) {
                b.append(',').append(buffers.get(index));
            }
        }
        b.append(']');
        return b.toString();
    }
}
