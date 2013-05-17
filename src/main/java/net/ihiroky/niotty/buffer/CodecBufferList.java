package net.ihiroky.niotty.buffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.GatheringByteChannel;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A {@link net.ihiroky.niotty.buffer.CodecBuffer} which consists of {@link net.ihiroky.niotty.buffer.CodecBuffer}s.
 * <p></p>
 * The {@link #addFirst(CodecBuffer)} and {@link #addLast(CodecBuffer)} add the argument into an internal list.
 * The elements contained in the list is sliced when added to. So an expansion of each elements does not happen.
 * This object allocates a new (unsliced) {@code CodecBuffer} and add it to the list if the object need expand
 * its space. The maximum elements that can be held by the object is 1024.
 *
 * @author Hiroki Itoh
 */
public class CodecBufferList extends AbstractCodecBuffer implements CodecBuffer {

    private List<CodecBuffer> buffers_;
    private CodecBufferFactory allocator_;
    private int beginningBufferIndex_;
    private int endBufferIndex_;
    private int priority_;

    private static final int INITIAL_BUFFERS_CAPACITY = 4;
    private static final int MAX_BUFFER_COUNT = 1024;

    private CodecBufferList(CodecBufferFactory allocator, int priority) {
        buffers_ = new ArrayList<>(INITIAL_BUFFERS_CAPACITY);
        allocator_ = allocator;
        priority_ = priority;
        endBufferIndex_ = -1;
    }

    CodecBufferList(CodecBufferFactory allocator, int priority, CodecBuffer buffer0, CodecBuffer... buffers) {
        Objects.requireNonNull(allocator, "allocator");
        Objects.requireNonNull(buffer0, "buffer0");
        Objects.requireNonNull(buffers, "buffers");
        if (buffers.length >= MAX_BUFFER_COUNT) {
            throw new IllegalArgumentException("length of buffers must be less than " + MAX_BUFFER_COUNT + ".");
        }

        List<CodecBuffer> list = new ArrayList<>(Math.max(INITIAL_BUFFERS_CAPACITY, buffers.length + 1));
        list.add(buffer0.slice());
        int end = 0;
        for (int i = 0; i < buffers.length; i++) {
            CodecBuffer b = buffers[i];
            if (b.remainingBytes() > 0) {
                end = i + 1;
            }
            list.add(b.slice());
        }
        buffers_ = list;
        allocator_ = allocator;
        priority_ = priority;
        endBufferIndex_ = end;
    }

    int beginningBufferIndex() {
        return beginningBufferIndex_;
    }

    int endBufferIndex() {
        return endBufferIndex_;
    }

    int sizeOfBuffers() {
        return buffers_.size();
    }

    @Override
    public boolean transferTo(GatheringByteChannel channel) throws IOException {
        List<CodecBuffer> buffers = buffers_;
        int offset = beginningBufferIndex_;
        int end = endBufferIndex_;
        for (; offset < end; offset++) {
            if (buffers.get(offset).remainingBytes() > 0) {
                break;
            }
        }
        if (offset == end && buffers.get(offset).remainingBytes() == 0) {
            return true;
        }
        ByteBuffer[] byteBuffers = new ByteBuffer[end - offset + 1];
        for (int i = offset; i <= end; i++) {
            byteBuffers[i - offset] = buffers.get(i).toByteBuffer();
        }
        channel.write(byteBuffers, 0, byteBuffers.length);
        for (int i = offset; i <= end; i++) {
            ByteBuffer byteBuffer = byteBuffers[i - offset];
            buffers.get(i).beginning(byteBuffer.position());
            if (byteBuffer.hasRemaining()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public CodecBufferList addFirst(CodecBuffer buffer) {
        if (buffer == null) {
            throw new IllegalArgumentException("buffer must not be null.");
        }

        int beginning = beginningBufferIndex_;
        while (beginning <= endBufferIndex_
                && buffers_.get(beginning).remainingBytes() == 0) {
            beginning = ++beginningBufferIndex_;
        }

        buffers_.add(beginning, buffer.slice());
        endBufferIndex_++;
        return this;
    }

    @Override
    public CodecBufferList addLast(CodecBuffer buffer) {
        if (buffer == null) {
            throw new IllegalArgumentException("buffer must not be null.");
        }

        int end = endBufferIndex_;
        while (end >= beginningBufferIndex_
                && buffers_.get(end).remainingBytes() == 0) {
            end--;
        }

        // TODO duplicate buffer to independent of beginning / end index.
        int size = buffers_.size();
        if (end == size - 1) { // end is the last index ?
            buffers_.add(buffer.slice());
            endBufferIndex_++;
        } else if (end >= 0) {
            buffers_.add(++end, buffer.slice());
            endBufferIndex_ = end;
        } else { // if (end == -1) { // all buffer between beginning and end are empty.
            buffers_.add(buffer.slice());
            endBufferIndex_ = (buffer.remainingBytes() > 0) ? size : beginningBufferIndex_;
        }
        return this;
    }

    private CodecBuffer appendNewCodecBuffer(CodecBuffer endBuffer, int expectedMinSize) {
        if (buffers_.size() >= MAX_BUFFER_COUNT) {
            throw new IllegalStateException("the size of buffers reaches maximum: " + MAX_BUFFER_COUNT);
        }
        CodecBuffer buffer = allocator_.newCodecBuffer(
                Math.max(endBuffer.capacityBytes() * EXPAND_MULTIPLIER, expectedMinSize));

        buffers_.add(buffer);
        endBufferIndex_++;
        return buffer;
    }

    private CodecBuffer nextBuffer(CodecBuffer current, int required) {
        return (endBufferIndex_ + 1 == buffers_.size())
                ? appendNewCodecBuffer(current, required) : buffers_.get(++endBufferIndex_);
    }

    @Override
    public void writeByte(int value) {
        CodecBuffer buffer = buffers_.get(endBufferIndex_);
        if (buffer.spaceBytes() == 0) {
            buffer = appendNewCodecBuffer(buffer, 1);
        }
        buffer.writeByte(value);
    }

    @Override
    public void writeBytes(byte[] bytes, int offset, int length) {
        CodecBuffer buffer = buffers_.get(endBufferIndex_);
        int space = buffer.spaceBytes();
        if (space >= length) {
            buffer.writeBytes(bytes, offset, length);
            return;
        }
        buffer.writeBytes(bytes, offset, space);

        length -= space;
        CodecBuffer newBuffer = appendNewCodecBuffer(buffer, length);
        newBuffer.writeBytes(bytes, offset + space, length);
    }

    @Override
    public void writeBytes(ByteBuffer byteBuffer) {
        CodecBuffer buffer = buffers_.get(endBufferIndex_);
        int space = buffer.spaceBytes();
        if (space >= byteBuffer.remaining()) {
            buffer.writeBytes(byteBuffer);
            return;
        }
        int limit = byteBuffer.limit();
        byteBuffer.limit(byteBuffer.position() + space);
        buffer.writeBytes(byteBuffer);
        byteBuffer.limit(limit);

        int writeBytesInNewBuffer = byteBuffer.remaining();
        CodecBuffer newBuffer = appendNewCodecBuffer(buffer, writeBytesInNewBuffer);
        newBuffer.writeBytes(byteBuffer);
    }

    @Override
    public void writeShort(short value) {
        CodecBuffer buffer = buffers_.get(endBufferIndex_);
        if (buffer.spaceBytes() >= CodecUtil.SHORT_BYTES) {
            buffer.writeShort(value);
            return;
        }
        CodecBuffer nextBuffer = nextBuffer(buffer, CodecUtil.SHORT_BYTES);
        nextBuffer.writeShort(value);
    }

    @Override
    public void writeChar(char value) {
        CodecBuffer buffer = buffers_.get(endBufferIndex_);
        if (buffer.spaceBytes() >= CodecUtil.CHAR_BYTES) {
            buffer.writeChar(value);
            return;
        }
        CodecBuffer newBuffer = nextBuffer(buffer, CodecUtil.CHAR_BYTES);
        newBuffer.writeChar(value);
    }

    @Override
    public void writeInt(int value) {
        CodecBuffer buffer = buffers_.get(endBufferIndex_);
        if (buffer.spaceBytes() >= CodecUtil.INT_BYTES) {
            buffer.writeInt(value);
            return;
        }
        writeShort((short) (value >>> CodecUtil.BYTE_SHIFT2));
        writeShort((short) (value & CodecUtil.SHORT_MASK));
    }

    @Override
    public void writeLong(long value) {
        CodecBuffer buffer = buffers_.get(endBufferIndex_);
        if (buffer.spaceBytes() >= CodecUtil.LONG_BYTES) {
            buffer.writeLong(value);
            return;
        }
        writeInt((int) (value >>> CodecUtil.BYTE_SHIFT4));
        writeInt((int) (value & CodecUtil.INT_MASK));
    }

    @Override
    public void writeString(String s, CharsetEncoder encoder) {
        Objects.requireNonNull(s, "s");
        Objects.requireNonNull(encoder, "encoder");

        CodecBuffer buffer = buffers_.get(endBufferIndex_);
        CharBuffer input = CharBuffer.wrap(s);
        ByteBuffer output = buffer.toByteBuffer();
        output.position(output.limit());
        output.limit(output.capacity());
        for (;;) {
            CoderResult cr = encoder.encode(input, output, true);
            if (cr.isUnderflow()) {
                cr = encoder.flush(output);
                if (cr.isUnderflow()) {
                    buffer.end(output.position());
                    encoder.reset();
                    break;
                }
                // jump ot overflow operation.
            }
            if (cr.isOverflow()) {
                buffer.end(output.position());
                if (endBufferIndex_ + 1 == buffers_.size()) {
                    buffer = appendNewCodecBuffer(
                            buffer, Buffers.outputByteBufferSize(encoder, input.remaining()));
                } else {
                    buffer = buffers_.get(++endBufferIndex_);
                }
                output = buffer.toByteBuffer();
                output.position(output.limit());
                output.limit(output.capacity());
                continue;
            }
            if (cr.isError()) {
                buffer.end(output.position());
                try {
                    cr.throwException();
                } catch (CharacterCodingException cce) {
                    throw new RuntimeException(cce);
                }
            }
        }
    }

    private CodecBuffer nextReadBufferOrNull() {
        CodecBuffer buffer = buffers_.get(beginningBufferIndex_);
        while (buffer.remainingBytes() == 0) {
            if (beginningBufferIndex_ >= endBufferIndex_) {
                return null;
            }
            buffer = buffers_.get(++beginningBufferIndex_);
        }
        return buffer;
    }

    private CodecBuffer nextReadBuffer() {
        CodecBuffer buffer = buffers_.get(beginningBufferIndex_);
        while (buffer.remainingBytes() == 0) {
            if (beginningBufferIndex_ >= endBufferIndex_) {
                throw new IndexOutOfBoundsException("No data remains.");
            }
            buffer = buffers_.get(++beginningBufferIndex_);
        }
        return buffer;
    }

    @Override
    public int readByte() {
        CodecBuffer buffer = nextReadBuffer();
        return buffer.readByte();
    }

    @Override
    public int readBytes(byte[] bytes, int offset, int length) {
        int space = length;
        while (space > 0) {
            CodecBuffer buffer = nextReadBufferOrNull();
            if (buffer == null) {
                break;
            }
            int remaining = buffer.remainingBytes();
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
            int remaining = buffer.remainingBytes();
            if (remaining > 0) {
                readTotal += buffer.readBytes(byteBuffer);
            }
        }
        return readTotal;
    }

    @Override
    public char readChar() {
        CodecBuffer buffer = nextReadBuffer();
        if (buffer.remainingBytes() >= CodecUtil.CHAR_BYTES) {
            return buffer.readChar();
        }
        return (char) ((readByte() << CodecUtil.BYTE_SHIFT1) | readByte());
    }

    @Override
    public short readShort() {
        CodecBuffer buffer = nextReadBuffer();
        if (buffer.remainingBytes() >= CodecUtil.SHORT_BYTES) {
            return buffer.readShort();
        }
        return (short) ((readByte() << CodecUtil.BYTE_SHIFT1) | readByte());
    }

    @Override
    public int readInt() {
        CodecBuffer buffer = nextReadBuffer();
        if (buffer.remainingBytes() >= CodecUtil.INT_BYTES) {
            return buffer.readInt();
        }
        return ((readShort() & CodecUtil.SHORT_MASK) << CodecUtil.BYTE_SHIFT2) | (readShort() & CodecUtil.SHORT_MASK);
    }

    @Override
    public long readLong() {
        CodecBuffer buffer = nextReadBuffer();
        if (buffer.remainingBytes() >= CodecUtil.LONG_BYTES) {
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
    public String readString(CharsetDecoder decoder, int bytes) {
        String cached = StringCache.getCachedValue(this, decoder, bytes);
        if (cached != null) {
            return cached;
        }

        CodecBuffer buffer = nextReadBuffer();
        ByteBuffer input = buffer.toByteBuffer();
        CharBuffer output = CharBuffer.allocate(Buffers.outputCharBufferSize(decoder, bytes));
        int currentRemaining = input.remaining();
        boolean endOfInput = currentRemaining >= bytes;
        int previousRemaining = 0;
        for (;;) {
            CoderResult cr = decoder.decode(input, output, endOfInput);
            if (cr.isUnderflow()) {
                if (endOfInput) {
                    cr = decoder.flush(output);
                    if (cr.isUnderflow()) {
                        buffer.beginning(input.position() - previousRemaining);
                        decoder.reset();
                        break;
                    }
                    // jump to overflow operation.
                } else {
                    final int remaining = input.remaining();
                    previousRemaining = remaining;
                    buffer.beginning(buffer.end());
                    buffer = nextReadBuffer();
                    if (remaining == 0) {
                        input = buffer.toByteBuffer();
                    } else {
                        ByteBuffer newInput = ByteBuffer.allocate(remaining + buffer.remainingBytes());
                        newInput.put(input).put(buffer.toByteBuffer()).flip();
                        input = newInput;
                    }
                    bytes -= currentRemaining - remaining;
                    currentRemaining = input.remaining();
                    endOfInput = currentRemaining >= bytes;
                    continue;
                }
            }
            if (cr.isOverflow()) {
                output = Buffers.expand(output, decoder, input.remaining());
                continue;
            }
            if (cr.isError()) {
                buffer.beginning(input.position() - previousRemaining);
                Buffers.throwRuntimeException(cr);
            }
        }
        output.flip();
        return StringCache.toString(output, decoder);
    }

    @Override
    public int skipBytes(int bytes) {
        int left = bytes;
        List<CodecBuffer> buffers = buffers_;
        int bufferIndex = beginningBufferIndex_;
        while (left != 0) {
            CodecBuffer buffer = buffers.get(bufferIndex);
            left -= buffer.skipBytes(left);
            if (bytes >= 0) {
                if (bufferIndex == endBufferIndex_) {
                    break;
                }
                bufferIndex++;
            } else {
                if (bufferIndex == 0) {
                    break;
                }
                bufferIndex--;
            }
        }
        beginningBufferIndex_ = bufferIndex;
        return bytes - left;
    }

    static int toNonNegativeInt(long value) {
        return (value <= Integer.MAX_VALUE) ? (int) value : Integer.MAX_VALUE;
    }

    @Override
    public int remainingBytes() {
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
        for (int i = beginningBufferIndex_; i <= end; i++) {
            sum += buffers.get(i).remainingBytes();
        }
        return sum;
    }

    @Override
    public int spaceBytes() {
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
            sum += buffers_.get(i).spaceBytes();
        }
        return sum;
    }

    @Override
    public int capacityBytes() {
        return toNonNegativeInt(capacityBytesLong());
    }

    /**
     * Returns the capacity of this buffer by the byte as long type.
     * @return the capacity of this buffer by the byte as long type.
     */
    public long capacityBytesLong() {
        long sum = 0;
        for (CodecBuffer buffer : buffers_) {
            sum += buffer.capacityBytes();
        }
        return sum;
    }

    @Override
    public int beginning() {
        return toNonNegativeInt(beginningLong());
    }

    /**
     * Returns the value of the beginning as long type.
     * @return the value of the beginning as long type.
     */
    public long beginningLong() {
        List<CodecBuffer> buffer = buffers_;
        int beginningBufferIndex = beginningBufferIndex_;
        long beginning = 0L;
        for (int i = 0; i < beginningBufferIndex; i++) {
            beginning += buffer.get(i).capacityBytes();
        }
        beginning += buffer.get(beginningBufferIndex).beginning();
        return beginning;
    }

    @Override
    public CodecBuffer beginning(int beginning) {
        return beginningLong(beginning);
    }

    /**
     * Sets the value of the beginning as long type.
     * {@inheritDoc}
     */
    public CodecBuffer beginningLong(long beginning) {
        if (beginning < 0) {
            throw new IllegalArgumentException("beginning must be more than 0.");
        }
        List<CodecBuffer> buffer = buffers_;
        int endBufferIndex = endBufferIndex_;
        CodecBuffer target = null;
        for (int bi = beginningBufferIndex_; bi <= endBufferIndex; bi++) {
            CodecBuffer b = buffer.get(bi);
            int c = b.capacityBytes();
            if (beginning < c) {
                target = b;
                break;
            }
            beginning -= c;
        }
        if (target == null) {
            throw new IllegalArgumentException("beginning is greater than end.");
        }
        target.beginning((int) beginning);
        return this;
    }

    @Override
    public int end() {
        return toNonNegativeInt(endLong());
    }

    /**
     * Returns the value of the end as long type.
     * @return the value of the end as long type.
     */
    public long endLong() {
        long sum = 0;
        int end = endBufferIndex_;
        for (int i = beginningBufferIndex_; i < end; i++) {
            sum += buffers_.get(i).capacityBytes();
        }
        sum += buffers_.get(end).end();
        return sum;
    }

    @Override
    public CodecBuffer end(int end) {
        return endLong(end);
    }

    /**
     * Sets the value of the end as long type.
     * {@inheritDoc}
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
            index += target.capacityBytes();
        }
        if (index < end) {
            throw new IndexOutOfBoundsException("end must be in the buffer: " + end);
        }
        if (target != null) {
            target.end((int) end);
        }
        return this;
    }

    @Override
    public int drainFrom(CodecBuffer buffer) {
        return drainFrom(buffer, buffer.remainingBytes());
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
            int space = b.spaceBytes();
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
        int wholeRemaining = remainingBytes();
        if (bytes <= 0 || bytes > wholeRemaining) {
            throw new IllegalArgumentException("Invalid input " + bytes + ". " + wholeRemaining + " byte remains.");
        }

        CodecBuffer buffer = nextReadBuffer();
        int remaining = buffer.remainingBytes();
        if (remaining >= bytes) {
            return buffer.slice(bytes);
        }
        CodecBufferList ccb = new CodecBufferList(allocator_, priority_);
        CodecBuffer sliced = buffer.slice(remaining);
        bytes -= remaining;
        ccb.addLast(sliced);
        while (bytes > 0) {
            buffer = nextReadBuffer();
            remaining = buffer.remainingBytes();
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
        CodecBufferList sliced = new CodecBufferList(allocator_, priority_);
        for (int i = beginningBufferIndex_; i <= endBufferIndex_; i++) {
            sliced.addLast(buffers_.get(i));
        }
        return sliced;
    }

    @Override
    public CodecBuffer duplicate() {
        CodecBufferList duplicated = new CodecBufferList(allocator_, priority_);
        duplicated.beginningBufferIndex_ = beginningBufferIndex_;
        duplicated.endBufferIndex_ = endBufferIndex_;
        for (CodecBuffer b : buffers_) {
            duplicated.buffers_.add(b.duplicate());
        }
        return duplicated;
    }

    @Override
    public CodecBuffer clear() {
        for (CodecBuffer b : buffers_) {
            b.clear();
        }
        beginningBufferIndex_ = 0;
        endBufferIndex_ = 0;
        return this;
    }

    @Override
    public ByteBuffer toByteBuffer() {
        int begin = beginningBufferIndex_;
        int end = endBufferIndex_;
        if (begin == end) {
            return buffers_.get(end).toByteBuffer();
        }
        long remaining = remainingBytesLong();
        if (remaining > Integer.MAX_VALUE) {
            throw new IllegalStateException("remaining is greater than Integer.MAX_VALUE.");
        }
        ByteBuffer byteBuffer = ByteBuffer.allocate((int) remaining);
        for (int i = begin; i <= end; i++) {
            CodecBuffer buffer = buffers_.get(i);
            byteBuffer.put(buffer.toByteBuffer());
        }
        byteBuffer.flip();
        return byteBuffer;
    }

    @Override
    public boolean hasArray() {
        if (beginningBufferIndex_ == endBufferIndex_) {
            CodecBuffer buffer = buffers_.get(endBufferIndex_);
            if (buffer.hasArray()) {
                return buffer.hasArray();
            }
        }
        return false;
    }

    @Override
    public byte[] toArray() {
        if (beginningBufferIndex_ == endBufferIndex_) {
            CodecBuffer buffer = buffers_.get(endBufferIndex_);
            if (buffer.hasArray()) {
                return buffer.toArray();
            }
        }
        throw new UnsupportedOperationException();
    }

    @Override
    public int arrayOffset() {
        if (beginningBufferIndex_ == endBufferIndex_) {
            CodecBuffer buffer = buffers_.get(endBufferIndex_);
            if (buffer.hasArray()) {
                return buffer.arrayOffset();
            }
        }
        throw new UnsupportedOperationException();
    }

    @Override
    public int indexOf(int b, int fromIndex) {
        if (fromIndex < 0) {
            fromIndex = 0;
        }

        List<CodecBuffer> buffers = buffers_;
        int begin = beginningBufferIndex_;
        int end = endBufferIndex_;
        long offset = 0;

        CodecBuffer buffer = null;
        while (begin <= end) {
            CodecBuffer cb = buffers.get(begin++);
            int remaining = cb.remainingBytes();
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
        offset += buffer.remainingBytes();
        for (; begin < end; begin++) {
            buffer = buffers.get(begin);
            index = buffer.indexOf(b, 0);
            if (index != -1) {
                return toNonNegativeInt(offset + index);
            }
            offset += buffer.remainingBytes();
        }
        return -1;
    }

    @Override
    public int indexOf(byte[] b, int fromIndex) {
        if (fromIndex < 0) {
            fromIndex = 0;
        }

        List<CodecBuffer> buffers = buffers_;
        int begin = beginningBufferIndex_;
        int end = endBufferIndex_;
        long offset = 0;

        CodecBuffer buffer = null;
        while (begin <= end) {
            CodecBuffer cb = buffers.get(begin++);
            int remaining = cb.remainingBytes();
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
        offset += buffer.remainingBytes();
        for (; begin < end; begin++) {
            buffer = buffers.get(begin);
            index = buffer.indexOf(b, 0);
            if (index != -1) {
                return toNonNegativeInt(offset + index);
            }
            offset += buffer.remainingBytes();
        }
        return -1;
    }

    @Override
    public int lastIndexOf(int b, int fromIndex) {
        List<CodecBuffer> buffers = buffers_;
        int begin = beginningBufferIndex_;
        int end = endBufferIndex_;
        int offset = remainingBytes(); // TODO long

        if (fromIndex >= offset) {
            fromIndex = offset;
        }
        CodecBuffer buffer;
        for (;;) {
            buffer = buffers.get(end--);
            offset -= buffer.remainingBytes();
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
        offset += buffer.remainingBytes();
        for (; end >= begin; end--) {
            buffer = buffers.get(begin);
            index = buffer.indexOf(b, 0);
            if (index != -1) {
                return offset + index;
            }
            offset += buffer.remainingBytes();
        }
        return -1;
    }

    @Override
    public int lastIndexOf(byte[] b, int fromIndex) {
        List<CodecBuffer> buffers = buffers_;
        int begin = beginningBufferIndex_;
        int end = endBufferIndex_;
        int offset = remainingBytes(); // TODO long

        CodecBuffer buffer;
        if (fromIndex >= offset) {
            fromIndex = offset;
        }
        for (;;) {
            buffer = buffers.get(end--);
            offset -= buffer.remainingBytes();
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
        offset += buffer.remainingBytes();
        for (; end >= begin; end--) {
            buffer = buffers.get(begin);
            index = buffer.indexOf(b, 0);
            if (index != -1) {
                return offset + index;
            }
            offset += buffer.remainingBytes();
        }
        return -1;
    }

    @Override
    public int priority() {
        return priority_;
    }

    @Override
    public void dispose() {
        for (CodecBuffer buffer : buffers_) {
            buffer.dispose();
        }
        buffers_.clear();
    }
}
