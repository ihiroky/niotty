package net.ihiroky.niotty.buffer;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CoderResult;
import java.nio.charset.MalformedInputException;
import java.nio.charset.UnmappableCharacterException;
import java.util.Collection;

/**
 *
 * @author Hiroki Itoh
 */
public final class Buffers {

    // TODO must be read only
    private static final EncodeBuffer NULL_ENCODE_BUFFER = new ArrayEncodeBuffer(0);

    private Buffers() {
        throw new AssertionError();
    }

    static int outputByteBufferSize(float bytesPerChar, int chars) {
        return (int) (bytesPerChar * chars) + 1;
    }

    static int outputCharBufferSize(float charsPerByte, int bytes) {
        return (int) (charsPerByte * bytes) + 1;
    }

    static CharBuffer expand(CharBuffer original, float charsPerByte, int remainingBytes) {
        CharBuffer t = CharBuffer.allocate(
                original.capacity() + Buffers.outputCharBufferSize(charsPerByte, remainingBytes));
        original.flip();
        t.put(original);
        return t;
    }

    static void throwRuntimeException(CoderResult coderResult) {
        if (coderResult.isMalformed()) {
            throw new RuntimeException(new MalformedInputException(coderResult.length()));
        } else if (coderResult.isUnmappable()) {
            throw new RuntimeException(new UnmappableCharacterException(coderResult.length()));
        }
    }

    public static EncodeBuffer emptyEncodeBuffer() {
        return NULL_ENCODE_BUFFER;
    }

    public static EncodeBuffer newEncodeBuffer() {
        return new ArrayEncodeBuffer();
    }

    public static EncodeBuffer newEncodeBuffer(int initialCapacity) {
        return new ArrayEncodeBuffer(initialCapacity);
    }

    public static EncodeBuffer newEncodeBuffer(byte[] buffer, int offset, int length) {
        return new ArrayEncodeBuffer(buffer, offset, length);
    }

    /**
     * Wraps a specified byte array into {@code DecodeBuffer}.
     *
     * The new {@code DecodeBuffer} is backed by the specified byte array. If some data is written into the
     * {@code DecodeBuffer}, then the backed byte array is also modified and vice versa. The new
     * {@code DecodeBuffer}'s limit is {@code offset + length} and the position is {@code offset}.
     *
     * @param buffer the backed byte array
     * @param offset the offset
     * @param length the content length in {@code b} from {@code offset},
     *               must be non-negative and less than or equal to {@code b.length - offset}
     * @return the new {@code DecodeBuffer}
     */
    public static DecodeBuffer newDecodeBuffer(byte[] buffer, int offset, int length) {
        return new ArrayDecodeBuffer(buffer, offset,  length);
    }

    /**
     * Wraps a specified {@code ByteBuffer} into {@code DecodeBuffer}.
     *
     * The new {@code DecodeBuffer} is backed by the specified {@code ByteBuffer}. If some data is written into the
     * {@code DecodeBuffer}, then the backed {@code ByteBuffer} is also modified and vice versa. The new
     * {@code DecodeBuffer}'s limit is {@code offset + length} and the position is {@code offset}.
     *
     * @param byteBuffer the backed {@code ByteBuffer}
     * @return the new {@code DecodeBuffer}
     */
    public static DecodeBuffer newDecodeBuffer(ByteBuffer byteBuffer) {
        return new ByteBufferDecodeBuffer(byteBuffer);
    }

    public static BufferSink createBufferSink(Collection<EncodeBuffer> encodeBuffers) {
        return new CompositeBufferSink(encodeBuffers);
    }
}
