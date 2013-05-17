package net.ihiroky.niotty.buffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.nio.ch.DirectBuffer;

import java.nio.ByteBuffer;

/**
 * A implementation of {@link net.ihiroky.niotty.buffer.ChunkPool} which manages
 * {@link net.ihiroky.niotty.buffer.ByteBufferChunk}. This class has maximum size to allocate the chunks.
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

    private Logger logger_ = LoggerFactory.getLogger(ByteBufferChunkPool.class);

    /**
     * Constructs a new instance.
     * @param maxPoolingBytes the size of a pre-allocated {@code ByteBuffer}; the maximum number of pooling by the byte.
     * @param direct true if the {@code ByteBuffer} is direct.
     * @throws IllegalArgumentException if the maxPoolingBytes is not positive.
     */
    ByteBufferChunkPool(int maxPoolingBytes, boolean direct) {
        if (maxPoolingBytes <= 0) {
            throw new IllegalArgumentException("maxPoolingBytes must be positive.");
        }
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
        try {
            if (whole_.isDirect()) {
                ((DirectBuffer) whole_).cleaner().clean();
            }
        } catch (Throwable t) {
            logger_.debug("[dispose] failed.", t);
        }
    }

    ByteBuffer wholeView() {
        return whole_.asReadOnlyBuffer();
    }
}
