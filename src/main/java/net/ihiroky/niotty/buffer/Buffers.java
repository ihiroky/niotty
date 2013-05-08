package net.ihiroky.niotty.buffer;

import java.io.IOException;
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
 * Provides factory methods of {@link net.ihiroky.niotty.buffer.CodecBuffer}
 * {@link net.ihiroky.niotty.buffer.BufferSink} and utility method for this package.
 *
 * @author Hiroki Itoh
 */
public final class Buffers {

    private Buffers() {
        throw new AssertionError();
    }

    /** Default priority (no wait). */
    static final int DEFAULT_PRIORITY = -1;

    private static final CodecBufferFactory ARRAY_CODEC_BUFFER_FACTORY = new CodecBufferFactory() {
        @Override
        public CodecBuffer newCodecBuffer(int bytes) {
            return new ArrayCodecBuffer(bytes);
        }
    };

    private static final CodecBufferFactory BYTE_ARRAY_CODEC_BUFFER_FACTORY = new CodecBufferFactory() {
        @Override
        public CodecBuffer newCodecBuffer(int bytes) {
            return new ByteBufferCodecBuffer(bytes);
        }
    };

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
     * Creates a new {@code CodecBuffer} which has initial capacity {@code initialCapacity} with a specified priority.
     * An invocation of this method behaves in exactly the same way as the invocation {@link #newCodecBuffer(int)}
     * if {@code priority < 0}.
     *
     * @param initialCapacity the initial capacity of the new {@code CodecBuffer}
     * @param priority buffer priority to choose a write queue
     * @return the new {@code CodecBuffer}
     */
    public static CodecBuffer newCodecBuffer(int initialCapacity, int priority) {
        return (priority < 0)
                ? newCodecBuffer(initialCapacity)
                : new PriorityArrayCodecBuffer(initialCapacity, priority);
    }

    /**
     * Creates a new {@code CodecBuffer} which is backed by a specified byte array.
     *
     * If some data is written into the {@code CodecBuffer}, then the backed byte array is also modified
     * and vice versa. The new {@code CodecBuffer}'s beginning is {@code offset} and end is {@code offset + length}.
     *
     * @param buffer the backed byte array
     * @param beginning the offset of content in {@code buffer}
     * @param length the length of content in {@code buffer} from {@code offset}
     * @return the new {@code DecodeBuffer}
     */
    public static CodecBuffer newCodecBuffer(byte[] buffer, int beginning, int length) {
        return new ArrayCodecBuffer(buffer, beginning, length);
    }

    /**
     * Creates a new {@code CodecBuffer} which is backed by a specified byte array with a specified priority.
     * An invocation of this method behaves in exactly the same way as the invocation
     * {@link #newCodecBuffer(byte[], int, int)} if {@code priority < 0}.
     *
     * @param buffer the backed byte array
     * @param beginning the offset of content in {@code buffer}
     * @param length the length of content in {@code buffer} from {@code offset}
     * @param priority buffer priority to choose a write queue
     * @return the new {@code DecodeBuffer}
     */
    public static CodecBuffer newCodecBuffer(byte[] buffer, int beginning, int length, int priority) {
        return (priority < 0)
                ? newCodecBuffer(buffer, beginning, length)
                : new PriorityArrayCodecBuffer(buffer, beginning, length, priority);
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
     * Creates a new {@code CodecBuffer} which is backed by a specified byte buffer.
     * An invocation of this method behaves in exactly the same way as the invocation
     * {@link #newCodecBuffer(java.nio.ByteBuffer)} if {@code priority < 0}.
     *
     * @param byteBuffer the backed {@code ByteBuffer}
     * @param priority buffer priority to choose a write queue
     * @return the new {@code DecodeBuffer}
     */
    public static CodecBuffer newCodecBuffer(ByteBuffer byteBuffer, int priority) {
        return (priority < 0) ? newCodecBuffer(byteBuffer) : new PriorityByteBufferCodecBuffer(byteBuffer, priority);
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
    public static FileBufferSink newBufferSink(Path path, long beginning, long length) throws IOException {
        FileChannel channel = FileChannel.open(path, StandardOpenOption.READ);
        return new FileBufferSink(channel, beginning, length, DEFAULT_PRIORITY);
    }

    /**
     * Creates a new {@code BufferSink} which presents a file specified with a {@code path} and its range.
     * An invocation of this method behaves in exactly the same way as the invocation
     * {@link #newBufferSink(java.nio.file.Path, long, long)} if {@code priority < 0}.
     *
     * @param path the path to points the file
     * @param beginning beginning byte of the range, from the head of the file
     * @param length byte length of the range
     * @param priority buffer priority to choose a write queue
     * @return a new {@code BufferSink} which presents the file
     * @throws IOException if failed to open the file
     */
    public static FileBufferSink newBufferSink(
            Path path, long beginning, long length, int priority) throws IOException {
        FileChannel channel = FileChannel.open(path, StandardOpenOption.READ);
        return new FileBufferSink(channel, beginning, length, priority);
    }

    /**
     * Creates a new {@code BufferSink} which holds a pair of {@code BufferSink}.
     * @param car the former one of the pair
     * @param cdr the latter one of the pair
     * @return a new {@code BufferSink} which holds a pair of {@code BufferSink}
     */
    public static BufferSink newBufferSink(BufferSink car, BufferSink cdr) {
        return new BufferSinkList(car, cdr);
    }

    /**
     * Creates a new {@code BufferSink} which holds a pair of {@code BufferSink}.
     * An invocation of this method behaves in exactly the same way as the invocation
     * {@link #newBufferSink(BufferSink, BufferSink)} if {@code priority < 0}.
     *
     * @param car the former one of the pair
     * @param cdr the latter one of the pair
     * @param priority buffer priority to choose a write queue
     * @return a new prioritized {@code BufferSink} which holds a pair of {@code BufferSink}
     */
    public static BufferSink newBufferSink(BufferSink car, BufferSink cdr, int priority) {
        return new BufferSinkList(car, cdr, priority);
    }

    public static CodecBuffer newCodecBuffer(CodecBuffer buffer0, CodecBuffer...buffers) {
        return new CodecBufferList(ARRAY_CODEC_BUFFER_FACTORY, DEFAULT_PRIORITY, buffer0, buffers);
    }

    public static CodecBuffer newCodecBuffer(CodecBufferFactory factory, CodecBuffer buffer0, CodecBuffer...buffers) {
        return new CodecBufferList(factory, DEFAULT_PRIORITY, buffer0, buffers);
    }

    public static CodecBuffer newCodecBuffer(
            CodecBufferFactory factory, int priority, CodecBuffer buffer0, CodecBuffer...buffers) {
        return new CodecBufferList(factory, priority, buffer0, buffers);
    }

    public static CodecBufferFactory newArrayCodecBufferFactory() {
        return ARRAY_CODEC_BUFFER_FACTORY;
    }

    public static CodecBufferFactory newArrayCodecBufferFactory(int wholeBytes) {
        final ArrayChunkPool pool = new ArrayChunkPool(wholeBytes);
        return new CodecBufferFactory() {
            ArrayChunkPool pool_ = pool;
            @Override
            public CodecBuffer newCodecBuffer(int bytes) {
                return new ArrayCodecBuffer(pool_, bytes);
            }
        };
    }

    public static CodecBufferFactory newByteBufferCodecBufferFactory() {
        return BYTE_ARRAY_CODEC_BUFFER_FACTORY;
    }

    public static CodecBufferFactory newByteBufferCodecBufferFactory(
            int wholeBytes, boolean direct) {
        final ByteBufferChunkPool pool = new ByteBufferChunkPool(wholeBytes, direct);
        return new CodecBufferFactory() {
            ByteBufferChunkPool pool_ = pool;
            @Override
            public CodecBuffer newCodecBuffer(int bytes) {
                return new ByteBufferCodecBuffer(pool_, bytes);
            }
        };
    }
}
