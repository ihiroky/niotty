package net.ihiroky.niotty.buffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

/**
 * @author Hiroki Itoh
 */
public abstract class ByteBufferChunkFactory extends ChunkManager<ByteBuffer> {

    private static final ByteBufferChunkFactory HEAP = new ByteBufferChunkFactory() {
        @Override
        public ByteBufferChunk newChunk(int bytes) {
            ByteBufferChunk c = new ByteBufferChunk(ByteBuffer.allocate(bytes), this);
            c.ready();
            return c;
        }
    };

    private static final ByteBufferChunkFactory DIRECT = new ByteBufferChunkFactory() {
        @Override
        public ByteBufferChunk newChunk(int bytes) {
            ByteBufferChunk c = new ByteBufferChunk(ByteBuffer.allocateDirect(bytes), this);
            c.ready();
            return c;
        }
    };

    private Logger logger_ = LoggerFactory.getLogger(ByteBufferChunkFactory.class);

    public static ByteBufferChunkFactory heap() {
        return HEAP;
    }

    public static ByteBufferChunkFactory direct() {
        return DIRECT;
    }

    public abstract ByteBufferChunk newChunk(int bytes);

    @Override
    protected void release(Chunk<ByteBuffer> chunk) {
        try {
            ((ByteBufferChunk) chunk).clear();
        } catch (Throwable t) {
            logger_.debug("[release] failed to release chunk.", t);
        }
    }

    @Override
    public void close() {
    }
}
