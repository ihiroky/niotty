package net.ihiroky.niotty.buffer;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CoderResult;
import java.nio.charset.MalformedInputException;
import java.nio.charset.UnmappableCharacterException;

/**
 * TODO comment against priority.
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
     * in exactly the same way as the invocation {@code newCodecBuffer(512)}
     * @return the new {@code CodecBuffer}.
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

    /**
     * Creates a new {@code CodecBuffer} which has initial capacity 512 with a specified priority.
     * An invocation of this method behaves in exactly the same way as the invocation {@link #newCodecBuffer()}
     * if {@code priority < 0}.
     *
     * @param priority buffer priority
     * @return the new {@code CodecBuffer}
     */
    public static CodecBuffer newPriorityCodecBuffer(int priority) {
        return (priority < 0) ? newCodecBuffer() : new PriorityArrayCodecBuffer(priority);
    }

    /**
     * Creates a new {@code CodecBuffer} which has initial capacity {@code initialCapacity} with a specified priority.
     * An invocation of this method behaves in exactly the same way as the invocation {@link #newCodecBuffer(int)}
     * if {@code priority < 0}.
     *
     * @param initialCapacity the initial capacity of the new {@code CodecBuffer}
     * @param priority buffer priority
     * @return the new {@code CodecBuffer}
     */
    public static CodecBuffer newPriorityCodecBuffer(int initialCapacity, int priority) {
        return (priority < 0)
                ? newCodecBuffer(initialCapacity)
                : new PriorityArrayCodecBuffer(initialCapacity, priority);
    }

    /**
     * Creates a new {@code CodecBuffer} which is backed by a specified byte array with a specified priority.
     * An invocation of this method behaves in exactly the same way as the invocation
     * {@link #newCodecBuffer(byte[], int, int)} if {@code priority < 0}.
     *
     * @param buffer the backed byte array
     * @param offset the offset of content in {@code buffer}
     * @param length the length of content in {@code buffer} from {@code offset}
     * @param priority buffer priority
     * @return the new {@code DecodeBuffer}
     */
    public static CodecBuffer newPriorityCodecBuffer(byte[] buffer, int offset, int length, int priority) {
        return (priority < 0)
                ? newCodecBuffer(buffer, offset, length)
                : new PriorityArrayCodecBuffer(buffer, offset, length, priority);
    }

    /**
     * Creates a new {@code CodecBuffer} which is backed by a specified byte buffer.
     * An invocation of this method behaves in exactly the same way as the invocation
     * {@link #newCodecBuffer(java.nio.ByteBuffer)} if {@code priority < 0}.
     *
     * @param byteBuffer the backed {@code ByteBuffer}
     * @param priority buffer priority
     * @return the new {@code DecodeBuffer}
     */
    public static CodecBuffer newPriorityCodecBuffer(ByteBuffer byteBuffer, int priority) {
        return (priority < 0) ? newCodecBuffer(byteBuffer) : new PriorityByteBufferCodecBuffer(byteBuffer, priority);
    }

}
