package net.ihiroky.niotty.buffer;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.nio.charset.MalformedInputException;
import java.nio.charset.UnmappableCharacterException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * Provides factory methods of {@link net.ihiroky.niotty.buffer.CodecBuffer},
 * {@link net.ihiroky.niotty.buffer.BufferSink} and utility method for this package.
 *
 * @author Hiroki Itoh
 */
public final class Buffers {

    private Buffers() {
        throw new AssertionError();
    }

    static final int DEFAULT_CAPACITY = 512;

    static int outputByteBufferSize(CharsetEncoder encoder, int chars) {
        return (int) Math.max(encoder.averageBytesPerChar() * chars, encoder.maxBytesPerChar()) + 1;
    }

    static int outputCharBufferSize(CharsetDecoder decoder, int bytes) {
        return (int) Math.max(decoder.averageCharsPerByte() * bytes, decoder.maxCharsPerByte()) + 1;
    }

    static CharBuffer expand(CharBuffer original, CharsetDecoder decoder, int remainingBytes) {
        CharBuffer t = CharBuffer.allocate(
                original.capacity() + Buffers.outputCharBufferSize(decoder, remainingBytes));
        original.flip();
        t.put(original);
        return t;
    }

    static void throwRuntimeException(CoderResult coderResult) {
        if (coderResult.isUnderflow()) {
            throw new IndexOutOfBoundsException("Buffer underflow.");
        } else if (coderResult.isMalformed()) {
            throw new RuntimeException(new MalformedInputException(coderResult.length()));
        } else if (coderResult.isUnmappable()) {
            throw new RuntimeException(new UnmappableCharacterException(coderResult.length()));
        }
        throw new AssertionError("Runtime should not reach here.");
    }

    /**
     * Creates a new {@code CodecBuffer} which has initial capacity 512.
     * The new {@code CodecBuffer} has no content to read.
     *
     * An invocation of this method behaves in exactly the same way as the invocation
     * {@code newCodecBuffer(512, DefaultTransportParameter.NO_PARAMETER)}.
     *
     * @return the new {@code CodecBuffer}.
     */
    public static CodecBuffer newCodecBuffer() {
        return new ArrayCodecBuffer(ArrayChunkFactory.instance(), DEFAULT_CAPACITY);
    }

    /**
     * Creates a new {@code CodecBuffer} which has initial capacity {@code initialCapacity}.
     * The new {@code CodecBuffer} has no content to read.
     *
     * @param initialCapacity the initial capacity of the new {@code CodecBuffer}.
     * @throws IllegalArgumentException if the initialCapacity is negative.
     * @return the new {@code CodecBuffer}
     */
    public static CodecBuffer newCodecBuffer(int initialCapacity) {
        return new ArrayCodecBuffer(ArrayChunkFactory.instance(), initialCapacity);
    }

    /**
     * Creates a new {@code CodecBuffer} which is backed by a specified byte array.
     *
     * If some data is written into the {@code CodecBuffer}, then the backed byte array is also modified.
     * The new {@code CodecBuffer}'s beginning is {@code 0} and end is {@code buffer.length}.
     *
     * An invocation of this method behaves in exactly the same way as the invocation
     * {@code wrap(buffer, 0, buffer.length)}.
     *
     * @param buffer the backed byte array
     * @throws NullPointerException if {@code buffer} is null.
     * @throws IllegalArgumentException if {@code beginning} or {@code length} is invalid.
     * @return the new {@code DecodeBuffer}
     */
    public static CodecBuffer wrap(byte[] buffer) {
        return new ArrayCodecBuffer(buffer, 0, buffer.length);
    }

    /**
     * Creates a new {@code CodecBuffer} which is backed by a specified byte array.
     *
     * If some data is written into the {@code CodecBuffer}, then the backed byte array is also modified.
     * The new {@code CodecBuffer}'s beginning is {@code offset} and end is {@code offset + length}.
     *
     * An invocation of this method behaves in exactly the same way as the invocation
     * {@code wrap(buffer, beginning, length, DefaultTransportParameter.NO_PARAMETER)}.
     *
     * @param buffer the backed byte array
     * @param offset the offset of content in {@code buffer}
     * @param length the length of content in {@code buffer} from {@code offset}
     * @throws NullPointerException if {@code buffer} is null.
     * @throws IllegalArgumentException if {@code beginning} or {@code length} is invalid.
     * @return the new {@code DecodeBuffer}
     */
    public static CodecBuffer wrap(byte[] buffer, int offset, int length) {
        return new ArrayCodecBuffer(buffer, offset, length);
    }

    /**
     * Creates a new {@code CodecBuffer} which has initial capacity {@code initialCapacity}.
     * The new {@code CodecBuffer} has no content to read.
     *
     * <p>An allocation of the new buffer's content is controlled by a specified {@code chunkPool}.</p>
     *
     * @param chunkPool the object which controls the allocation of the new buffer's content.
     * @param initialCapacity the initial capacity of the new {@code CodecBuffer}.
     * @throws IllegalArgumentException if the initialCapacity is negative.
     * @return the new {@code CodecBuffer}
     */
    public static CodecBuffer newCodecBuffer(ArrayChunkPool chunkPool, int initialCapacity) {
        return new ArrayCodecBuffer(chunkPool, initialCapacity);
    }

    /**
     * Creates a new {@code CodecBuffer} which is backed by a specified byte buffer.
     *
     * <p>If some data is written into the {@code CodecBuffer}, then the backed {@code ByteBuffer} is also modified.
     * The new {@code CodecBuffer}'s beginning is buffer' position and end is buffer's limit.</p>
     *
     * An invocation of this method behaves in exactly the same way as the invocation
     * {@code wrap(byteBuffer, false)}.
     *
     * @param byteBuffer the backed {@code ByteBuffer}
     * @return the new {@code CodecBuffer}
     */
    public static CodecBuffer wrap(ByteBuffer byteBuffer) {
        return wrap(byteBuffer, false);
    }

    /**
     * Creates a new {@code CodecBuffer} which is backed by a specified byte buffer.
     *
     * <p>If some data is written into the {@code CodecBuffer}, then the backed {@code ByteBuffer} is also modified.
     * The new {@code CodecBuffer}'s beginning is buffer' position and end is buffer's limit.</p>
     *
     * @param byteBuffer the backed {@code ByteBuffer}
     * @param cleanOnDispose true if {@code java.nio.DirectBuffer#clean()} is called
     *                       when {@link net.ihiroky.niotty.buffer.CodecBuffer#dispose()} is called;
     *                       this has an effect when {@code byteBuffer} is direct buffer.
     * @return the new {@code CodecBuffer}
     */
    public static CodecBuffer wrap(ByteBuffer byteBuffer, boolean cleanOnDispose) {
        return new ByteBufferCodecBuffer(byteBuffer, cleanOnDispose);
    }

    /**
     * Creates a new {@code CodecBuffer} which is backed by a specified byte buffer.
     *
     * <p>If some data is written into the {@code DecodeBuffer}, then the backed {@code ByteBuffer} is also modified.
     * The new {@code DecodeBuffer}'s beginning is buffer' position and end is buffer's limit.</p>
     *
     * <p>An allocation of the new buffer's content is controlled by a specified {@code chunkPool}.</p>
     *
     * @param chunkPool the object which controls the allocation of the new buffer's content.
     * @param initialCapacity the initial capacity of the new {@code CodecBuffer}.
     * @return the new {@code CodecBuffer}
     */
    public static CodecBuffer newCodecBuffer(ByteBufferChunkPool chunkPool, int initialCapacity) {
        return new ByteBufferCodecBuffer(chunkPool, initialCapacity);
    }

    /**
     * Creates a new {@code BufferSink} which presents a file specified with a {@code path} and its range.
     *
     * @param path the path to points the file
     * @param beginning beginning byte of the range, from the head of the file
     * @param length byte length of the range
     * @return a new {@code BufferSink} which presents the file
     * @throws IOException if failed to open the file
     */
    public static BufferSink newBufferSink(Path path, long beginning, long length) throws IOException {
        FileChannel channel = FileChannel.open(path, StandardOpenOption.READ);
        return new FileBufferSink(channel, beginning, length);
    }

    /**
     * Creates a new {@code BufferSink} which presents a file specified with a {@code file} and its range.
     *
     * @param file the file to points the file
     * @param beginning beginning byte of the range, from the head of the file
     * @param length byte length of the range
     * @return a new {@code BufferSink} which presents the file
     * @throws IOException if failed to open the file
     */
    public static BufferSink newBufferSink(File file, long beginning, long length) throws IOException {
        FileChannel channel = new RandomAccessFile(file, "r").getChannel();
        return new FileBufferSink(channel, beginning, length);
    }

    /**
     * Creates a new {@code BufferSink} which holds a pair of {@code BufferSink}.
     *
     * @param car the former one of the pair
     * @param cdr the latter one of the pair
     * @return a new {@code BufferSink} which holds a pair of {@code BufferSink}
     */
    public static BufferSink wrap(BufferSink car, BufferSink cdr) {
        return new BufferSinkList(car, cdr);
    }

    /**
     * Creates a new {@code CodecBuffer} which consists of the specified {@code buffer}.
     *
     * <p>The new buffer allocates a new {@code CodecBuffer} in the heap if the object needs more space
     * on write operations. The maximum elements that can be held by the object is 1024.</p>
     *
     * @param buffer the first {@code CodecBuffer} in the new {@code CodecBuffer}.
     * @return the new {@code CodecBuffer}.
     */
    public static CodecBuffer wrap(CodecBuffer buffer) {
        return new CodecBufferList(buffer);
    }

    /**
     * Creates a new {@code CodecBuffer} which consists of the specified {@code first} and {@code second}.
     * The order of {@code CodecBuffer} in the new buffer is the argument order; the first is {@code first},
     * the second is {@code second}.
     *
     * <p>The new buffer allocates a new {@code CodecBuffer} in the heap if the object needs more space
     * on write operations. The maximum elements that can be held by the object is 1024.</p>
     *
     * @param first the first {@code CodecBuffer} in the new {@code CodecBuffer}.
     * @param second the {@code CodecBuffer} after the {@code first}.
     * @return the new {@code CodecBuffer}.
     */
    public static CodecBuffer wrap(CodecBuffer first, CodecBuffer second) {
        return new CodecBufferList(first, second);
    }

    /**
     * Creates a new {@code CodecBuffer} which consists of the specified  {@code buffers}.
     * The order of {@code CodecBuffer} in the new buffer is the argument order.
     *
     * <p>The new buffer allocates a new {@code CodecBuffer} in the heap if the object needs more space
     * on write operations. The maximum elements that can be held by the object is 1024.</p>
     *
     * @param buffers the buffers.
     * @return the new {@code CodecBuffer}.
     */
    public static CodecBuffer wrap(CodecBuffer... buffers) {
        return new CodecBufferList(buffers);
    }

    /**
     * Creates a new {@code CodecBuffer} which content is drained from the specified buffer
     * with specified length. The beginning of the drained buffer increases by the length.
     *
     * @param buffer the buffer to be drained to the new buffer
     * @param length the length to be drained from the buffer
     * @return the new buffer
     */
    public static CodecBuffer newCodecBuffer(CodecBuffer buffer, int length) {
        CodecBuffer b = Buffers.wrap(new byte[length], 0, 0);
        b.drainFrom(buffer, length);
        return b;
    }
}
