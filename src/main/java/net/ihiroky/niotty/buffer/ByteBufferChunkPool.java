package net.ihiroky.niotty.buffer;

import net.ihiroky.niotty.util.Arguments;
import net.ihiroky.niotty.util.Platform;

import java.nio.ByteBuffer;

/**
 * A implementation of {@link ChunkPool} which manages
 * {@link ByteBufferChunk}. This class has maximum size to allocate the chunks.
 * If total size of the pooled chunks exceeds the size, unpooled chunks are allocated, which is always
 * in the heap space.
 * <p></p>
 * TODO MBean support
 * @author Hiroki Itoh
 */
public class ByteBufferChunkPool extends ChunkPool<ByteBuffer> {

    /**
     * A pre-allocated {@code ByteBuffer} to slice a content in a chunk.
     * TODO support multiple ByteBuffer
     */
    private final ByteBuffer whole_;

    /**
     * Constructs a new instance.
     * The chunk created by the new instance has direct {@code ByteBuffer}.
     *
     * @param maxPoolingBytes the size of a pre-allocated {@code ByteBuffer}; the maximum number of pooling by the byte.
     * @throws IllegalArgumentException if the maxPoolingBytes is not positive.
     */
    public ByteBufferChunkPool(int maxPoolingBytes) {
        this(maxPoolingBytes, true);
    }

    /**
     * Constructs a new instance.
     * @param maxPoolingBytes the size of a pre-allocated {@code ByteBuffer}; the maximum number of pooling by the byte.
     * @param direct true if the {@code ByteBuffer} is direct.
     * @throws IllegalArgumentException if the maxPoolingBytes is not positive.
     */
    public ByteBufferChunkPool(int maxPoolingBytes, boolean direct) {
        Arguments.requirePositive(maxPoolingBytes, "maxPoolingBytes");
        whole_ = direct ? ByteBuffer.allocateDirect(maxPoolingBytes) : ByteBuffer.allocate(maxPoolingBytes);
    }

    @Override
    protected ByteBufferChunk allocate(int bytes) {
        ByteBufferChunk chunk;
        synchronized (whole_) {
            if (bytes > whole_.remaining()) {
                chunk = new ByteBufferChunk(ByteBuffer.allocate(bytes), ByteBufferChunkFactory.heap());
            } else {
                int limit = whole_.position() + bytes;
                whole_.limit(limit);
                ByteBuffer sliced = whole_.slice();
                whole_.position(limit).limit(whole_.capacity());
                chunk = new ByteBufferChunk(sliced, this);
            }
        }
        return chunk;
    }

    /**
     * Clears the pre-allocated {@code ByteBuffer} if the {@code ByteBuffer} is direct.
     */
    @Override
    protected void dispose() {
        Platform.release(whole_);
    }

    ByteBuffer wholeView() {
        return whole_.asReadOnlyBuffer();
    }
}
