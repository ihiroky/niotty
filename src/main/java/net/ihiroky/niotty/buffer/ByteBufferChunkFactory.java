package net.ihiroky.niotty.buffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

/**
 * A factory class that creates {@link net.ihiroky.niotty.buffer.ByteBufferChunk}.
 * Use {@link #heap()} to get an instance which creates a heap {@code ByteBuffer}.
 * If a direct {@code ByteBuffer} is needed, use {@link #direct()}.
 * <p></p>
 * This class does not provide a pooling feature, so unlimited use of the direct buffer allocation
 * is not recommended. It probably cause an out of memory error.
 *
 * @author Hiroki Itoh
 */
public abstract class ByteBufferChunkFactory extends ChunkManager<ByteBuffer> {

    private static final ByteBufferChunkFactory HEAP = new ByteBufferChunkFactory() {
        @Override
        protected ByteBufferChunk newChunk(int bytes) {
            ByteBufferChunk c = new ByteBufferChunk(ByteBuffer.allocate(bytes), this);
            c.ready();
            return c;
        }
    };

    private static final ByteBufferChunkFactory DIRECT_RELEASE = new ByteBufferChunkFactory() {
        private Logger logger_ = LoggerFactory.getLogger(ByteBufferChunkFactory.class);
        @Override
        protected ByteBufferChunk newChunk(int bytes) {
            ByteBufferChunk c = new ByteBufferChunk(ByteBuffer.allocateDirect(bytes), this);
            c.ready();
            return c;
        }
        @Override
        protected void release(Chunk<ByteBuffer> chunk) {
            try {
                ((ByteBufferChunk) chunk).clear();
            } catch (Throwable t) {
                logger_.debug("[release] failed to release chunk.", t);
            }
        }
    };

    private static final ByteBufferChunkFactory DIRECT_NO_RELEASE = new ByteBufferChunkFactory() {
        @Override
        protected ByteBufferChunk newChunk(int bytes) {
            ByteBufferChunk c = new ByteBufferChunk(ByteBuffer.allocateDirect(bytes), this);
            c.ready();
            return c;
        }
    };

    /**
     * Returns the instance of this class that creates heap {@code ByteBufferChunk}.
     * @return the instance of this class that creates heap {@code ByteBufferChunk}.
     */
    public static ByteBufferChunkFactory heap() {
        return HEAP;
    }

    /**
     * Returns the instance of this class that creates direct {@code ByteBufferChunk}.
     * {@code java.nio.DirectBuffer#clean()} is called when {@link #release(Chunk)} is called.
     * @return the instance of this class that creates direct {@code ByteBufferChunk}.
     */
    public static ByteBufferChunkFactory direct() {
        return direct(false);
    }

    /**
     * Returns the instance of this class that creates direct {@code ByteBufferChunk}.
     * @param cleanOnRelease true if {@code java.nio.DirectBuffer#clean()} is called
     *                       when {@link #release(Chunk)} is called.
     * @return the instance of this class that creates direct {@code ByteBufferChunk}.
     */
    public static ByteBufferChunkFactory direct(boolean cleanOnRelease) {
        return cleanOnRelease ? DIRECT_RELEASE : DIRECT_NO_RELEASE;
    }

    /**
     * {@inheritDoc}
     * @return the chunk that has an {@code ByteBuffer}.
     */
    protected abstract ByteBufferChunk newChunk(int bytes);

    @Override
    protected void release(Chunk<ByteBuffer> chunk) {
    }

    /**
     * Does nothing.
     */
    @Override
    public void close() {
    }
}
