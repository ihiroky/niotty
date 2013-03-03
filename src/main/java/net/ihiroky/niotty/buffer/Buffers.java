package net.ihiroky.niotty.buffer;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CoderResult;
import java.nio.charset.MalformedInputException;
import java.nio.charset.UnmappableCharacterException;

/**
 *
 * @author Hiroki Itoh
 */
public final class Buffers {

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

    /**
     * Creates a new {@code CodecBuffer} which has initial capacity 512.
     * The new {@code CodecBuffer} has no content to read. An invocation of this method behaves
     * in exactly the same way as the invocation {@code new}
     * @return the new {@code newCodecBuffer(512)}.
     */
    public static CodecBuffer newCodecBuffer() {
        return new ArrayCodecBuffer();
    }

    /**
     * Creates a new {@code CodecBuffer} which has initial capacity {@code initialCapacity}.
     * The new {@code CodecBuffer} has no content to read. An invocation of this method of the form
     * {@code newCodecBuffer(n)} behaves in exactly the same way as the invocation
     * {@code newCodecBuffer(new byte[n], 0, 0)}.
     *
     * @param initialCapacity the initial capacity of the new {@code CodecBuffer}.
     * @return the new {@code CodecBuffer}
     */
    public static CodecBuffer newCodecBuffer(int initialCapacity) {
        return new ArrayCodecBuffer(initialCapacity);
    }

    /**
     * Creates a new {@code CodecBuffer} which is backed by a specified byte array.
     *
     * If some data is written into the {@code CodecBuffer}, then the backed byte array is also modified
     * and vice versa. The new {@code CodecBuffer}'s beginning is {@code offset} and end is {@code offset + length}.
     *
     * @param buffer the backed byte array
     * @param offset the offset of content in {@code buffer}
     * @param length the length of content in {@code buffer} from {@code offset}
     * @return the new {@code DecodeBuffer}
     */
    public static CodecBuffer newCodecBuffer(byte[] buffer, int offset, int length) {
        return new ArrayCodecBuffer(buffer, offset, length);
    }

    /**
     * Creates a new {@code CodecBuffer} which is backed by a specified byte buffer.
     *
     * If some data is written into the {@code DecodeBuffer}, then the backed {@code ByteBuffer} is also modified
     * and vice versa. The new {@code DecodeBuffer}'s beginning is buffer' position and end is buffer's limit.
     *
     * @param byteBuffer the backed {@code ByteBuffer}
     * @return the new {@code DecodeBuffer}
     */
    public static CodecBuffer newCodecBuffer(ByteBuffer byteBuffer) {
        return new ByteBufferCodecBuffer(byteBuffer);
    }
}
