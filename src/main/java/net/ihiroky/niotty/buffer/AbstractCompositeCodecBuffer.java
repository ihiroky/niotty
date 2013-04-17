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

/**
 * @author Hiroki Itoh
 */
public abstract class AbstractCompositeCodecBuffer extends AbstractCodecBuffer implements CodecBuffer {

    private List<CodecBuffer> buffers_ = new ArrayList<>(INITIAL_GROUP_CAPACITY);
    private int beginningBufferIndex_;
    private int endBufferIndex_;
    private int priority_;

    private static final int INITIAL_GROUP_CAPACITY = 4; // actually 8 in ArrayDeque.

    public AbstractCompositeCodecBuffer() {
        priority_ = Buffers.DEFAULT_PRIORITY;
    }

    public AbstractCompositeCodecBuffer(int priority) {
        priority_ = priority;
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
        if (offset == end) {
            return true;
        }
        ByteBuffer[] byteBuffers = new ByteBuffer[end - offset];
        for (int i = offset; i < end; i++) {
            byteBuffers[i - offset] = buffers.get(i).toByteBuffer();
        }
        channel.write(byteBuffers, 0, byteBuffers.length);
        for (int i = offset; i < end; i++) {
            ByteBuffer byteBuffer = byteBuffers[i - offset];
            buffers.get(i).beginning(byteBuffer.position());
            if (byteBuffer.hasRemaining()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public AbstractCompositeCodecBuffer addFirst(CodecBuffer buffer) {
        buffers_.add(0, buffer);
        return this;
    }

    @Override
    public AbstractCompositeCodecBuffer addLast(CodecBuffer buffer) {
        buffers_.add(buffer);
        return this;
    }

    private CodecBuffer appendNewCodecBuffer(CodecBuffer endBuffer, int expectedMinSize) {
        CodecBuffer buffer = newCodecBuffer(Math.max(endBuffer.capacityBytes() * 2, expectedMinSize));
        buffers_.add(buffer);
        endBufferIndex_++;
        return buffer;
    }

    protected abstract CodecBuffer newCodecBuffer(int capacity);

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
        int writeBytesInNewBuffer = length - space;
        CodecBuffer newBuffer = appendNewCodecBuffer(buffer, writeBytesInNewBuffer);
        buffer.writeBytes(bytes, offset, space);
        newBuffer.writeBytes(bytes, offset + space, writeBytesInNewBuffer);
    }

    @Override
    public void writeBytes(ByteBuffer byteBuffer) {
        CodecBuffer buffer = buffers_.get(endBufferIndex_);
        int space = buffer.spaceBytes();
        int remaining = byteBuffer.remaining();
        if (space >= remaining) {
            buffer.writeBytes(byteBuffer);
            return;
        }
        int writeBytesInNewBuffer = remaining - space;
        CodecBuffer newBuffer = appendNewCodecBuffer(buffer, writeBytesInNewBuffer);
        int limit = byteBuffer.limit();
        byteBuffer.limit(byteBuffer.position() + space);
        buffer.writeBytes(byteBuffer);
        byteBuffer.limit(limit);
        newBuffer.writeBytes(byteBuffer);
    }

    @Override
    public void writeShort(short value) {
        CodecBuffer buffer = buffers_.get(endBufferIndex_);
        if (buffer.spaceBytes() >= CodecUtil.SHORT_BYTES) {
            buffer.writeShort(value);
            return;
        }
        CodecBuffer newBuffer = appendNewCodecBuffer(buffer, CodecUtil.SHORT_BYTES);
        buffer.writeByte(value >>> CodecUtil.BYTE_SHIFT1);
        newBuffer.writeByte(value & CodecUtil.BYTE_MASK);
    }

    @Override
    public void writeChar(char value) {
        CodecBuffer buffer = buffers_.get(endBufferIndex_);
        if (buffer.spaceBytes() >= CodecUtil.CHAR_BYTES) {
            buffer.writeChar(value);
            return;
        }
        CodecBuffer newBuffer = appendNewCodecBuffer(buffer, CodecUtil.SHORT_BYTES);
        buffer.writeByte(value >>> CodecUtil.BYTE_SHIFT1);
        newBuffer.writeByte(value & CodecUtil.BYTE_MASK);
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
        if (buffer.spaceBytes() >= CodecUtil.INT_BYTES) {
            buffer.writeLong(value);
            return;
        }
        writeInt((int) (value >>> CodecUtil.BYTE_SHIFT4));
        writeInt((int) (value & CodecUtil.INT_BYTES));
    }

    @Override
    public void writeString(CharsetEncoder charsetEncoder, String s) {
        if (s == null) {
            throw new NullPointerException("s must not be null.");
        }

        if (StringCache.writeEmptyOrOneCharAscii(this, charsetEncoder, s)) {
            return;
        }

        // TODO ここのVBCはunsignedにしたい unsigned int 版 VBC
        final int maxVariableBytesLength = 5;
        CodecBuffer buffer = buffers_.get(endBufferIndex_);
        if (buffer.spaceBytes() < maxVariableBytesLength) {
            buffer = appendNewCodecBuffer(buffer, maxVariableBytesLength);
        }
        CodecBuffer firstBuffer = buffer;
        int firstPosition = firstBuffer.end();
        firstBuffer.end(buffer.end() + maxVariableBytesLength);

        CharBuffer input = CharBuffer.wrap(s);
        ByteBuffer output = buffer.toByteBuffer();
        int startPosition = output.position();
        int outputLength = 0;
        for (;;) {
            CoderResult cr = charsetEncoder.encode(input, output, true);
            if (cr.isUnderflow()) {
                cr = charsetEncoder.flush(output);
                if (cr.isUnderflow()) {
                    buffer.end(output.position());
                    outputLength += output.position() - startPosition;
                    charsetEncoder.reset();
                    break;
                }
                // jump ot overflow operation.
            }
            if (cr.isOverflow()) {
                buffer.end(output.position());
                outputLength += output.position() - startPosition;
                buffer = appendNewCodecBuffer(
                        buffer, (int) (charsetEncoder.averageBytesPerChar() * input.remaining() + 1));
                output = buffer.toByteBuffer();
                startPosition = output.position();
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
        int variableBytesLength = CodecUtil.variableByteLength(outputLength);
        int end = firstBuffer.end();
        firstBuffer.end(firstPosition + (maxVariableBytesLength - variableBytesLength));
        firstBuffer.writeVariableByteInteger(outputLength);
        firstBuffer.end(end);
    }

    private CodecBuffer nextReadBufferOrNull() {
        CodecBuffer buffer = buffers_.get(beginningBufferIndex_);
        while (buffer.remainingBytes() == 0) {
            if (beginningBufferIndex_ >= endBufferIndex_) {
                throw new IndexOutOfBoundsException("No data remains.");
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
    public String readString(CharsetDecoder charsetDecoder, int bytes) {
        String cached = StringCache.getCachedValue(this, charsetDecoder, bytes);
        if (cached != null) {
            return cached;
        }

        float charsPerByte = charsetDecoder.averageCharsPerByte();
        CodecBuffer buffer = nextReadBuffer();
        ByteBuffer input = buffer.toByteBuffer();
        CharBuffer output = CharBuffer.allocate(Buffers.outputCharBufferSize(charsPerByte, bytes));
        int currentRemaining = input.remaining();
        boolean endOfInput = currentRemaining >= bytes;
        int previousRemaining = 0;
        for (;;) {
            CoderResult cr = charsetDecoder.decode(input, output, endOfInput);
            if (cr.isUnderflow()) {
                if (endOfInput) {
                    cr = charsetDecoder.flush(output);
                    if (cr.isUnderflow()) {
                        buffer.beginning(input.position() - previousRemaining);
                        charsetDecoder.reset();
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
                        input = ByteBuffer.allocate(remaining + buffer.remainingBytes());
                        input.put(input).put(buffer.toByteBuffer());
                        input.flip();
                    }
                    bytes -= currentRemaining - remaining;
                    currentRemaining = input.remaining();
                    endOfInput = currentRemaining >= bytes;
                    continue;
                }
            }
            if (cr.isOverflow()) {
                output = Buffers.expand(output, charsPerByte, input.remaining());
                continue;
            }
            if (cr.isError()) {
                buffer.beginning(input.position() - previousRemaining);
                Buffers.throwRuntimeException(cr);
            }
        }
        output.flip();
        return StringCache.toString(output, charsetDecoder);
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
                    beginningBufferIndex_ = bufferIndex;
                    break;
                }
                bufferIndex++;
            } else {
                if (bufferIndex == 0) {
                    beginningBufferIndex_ = bufferIndex;
                    break;
                }
                bufferIndex--;
            }
        }
        return bytes - left;
    }

    static int toNonNegativeInt(long value) {
        return (value <= Integer.MAX_VALUE) ? (int) value : Integer.MAX_VALUE;
    }

    @Override
    public int remainingBytes() {
        return toNonNegativeInt(remainingBytesLong());
    }

    public long remainingBytesLong() {
        long sum = 0;
        List<CodecBuffer> buffers = buffers_;
        int end = endBufferIndex_;
        for (int i = beginningBufferIndex_; i < end; i++) {
            sum += buffers.get(i).remainingBytes();
        }
        return sum;
    }

    @Override
    public int spaceBytes() {
        return toNonNegativeInt(spaceBytesLong());
    }

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

    public CodecBuffer beginningLong(long beginning) {
        if (beginning < 0) {
            throw new IllegalArgumentException("beginning must be more than 0.");
        }
        List<CodecBuffer> buffer = buffers_;
        int endBufferIndex = endBufferIndex_;
        CodecBuffer target = null;
        for (int bi = 0; bi < endBufferIndex; bi++) {
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
        long sum = 0;
        int end = endBufferIndex_;
        for (int i = beginningBufferIndex_; i < end; i++) {
            sum += buffers_.get(i).capacityBytes();
        }
        sum += buffers_.get(end).remainingBytes();
        return (sum <= Integer.MAX_VALUE) ? (int) sum : Integer.MAX_VALUE;
    }

    @Override
    public CodecBuffer end(int end) {
        return endLong(end);
    }

    public CodecBuffer endLong(long end) {
        List<CodecBuffer> buffer = buffers_;
        int size = buffer.size();
        CodecBuffer target = null;
        long index = 0;
        for (int bi = 0; bi < size; bi++) {
            target = buffer.get(bi);
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
        CodecBuffer b = buffers_.get(endBufferIndex_);
        int space = b.spaceBytes();
        if (space >= bytes) {
            return b.drainFrom(buffer, bytes);
        }
        int writeInNewBuffer = bytes - space;
        CodecBuffer newBuffer = appendNewCodecBuffer(b, writeInNewBuffer);
        b.drainFrom(buffer, space);
        newBuffer.drainFrom(buffer, writeInNewBuffer);
        return bytes;
    }

    @Override
    public CodecBuffer slice(int bytes) {
        CodecBuffer buffer = nextReadBuffer();
        int remaining = buffer.remainingBytes();
        if (remaining >= bytes) {
            return buffer.slice(bytes);
        }
        AbstractCompositeCodecBuffer ccb = null; // TODO
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
        return this;
    }

    @Override
    public CodecBuffer clear() {
        int size = buffers_.size();
        for (CodecBuffer b : buffers_) {
            b.clear();
        }
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

        CodecBuffer buffer;
        for (;;) {
            buffer = buffers.get(begin++);
            int remaining = buffer.remainingBytes();
            if (remaining + offset >= fromIndex) {
                break;
            }
            offset += remaining;
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

        CodecBuffer buffer;
        for (;;) {
            buffer = buffers.get(begin++);
            int remaining = buffer.remainingBytes();
            if (remaining + offset >= fromIndex) {
                break;
            }
            if (begin > end) {
                return -1;
            }
            offset += remaining;
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

        CodecBuffer buffer;
        if (fromIndex >= offset) {
            fromIndex = (int) offset;
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
    public int lastIndexOf(byte[] b, int fromIndex) {
        List<CodecBuffer> buffers = buffers_;
        int begin = beginningBufferIndex_;
        int end = endBufferIndex_;
        int offset = remainingBytes(); // TODO long

        CodecBuffer buffer;
        if (fromIndex >= offset) {
            fromIndex = (int) offset;
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
