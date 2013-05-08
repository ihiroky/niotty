package net.ihiroky.niotty.buffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

/**
 * @author Hiroki Itoh
 */
public enum ByteBufferChunkFactory implements ChunkManager<ByteBuffer> {

    HEAP {
        @Override
        public ByteBufferChunk newChunk(int bytes) {
            return new ByteBufferChunk(ByteBuffer.allocate(bytes), this);
        }
    },
    DIRECT {
        @Override
        public ByteBufferChunk newChunk(int bytes) {
            return new ByteBufferChunk(ByteBuffer.allocateDirect(bytes), this);
        }
    },
    ;

    private Logger logger_ = LoggerFactory.getLogger(ByteBufferChunkFactory.class);

    @Override
    public void release(Chunk<ByteBuffer> chunk) {
        try {
            ((ByteBufferChunk) chunk).clear();
        } catch (Throwable t) {
            logger_.debug("[release] failed to release chunk.", t);
        }
    }

    @Override
    public void close() {
    }

    public abstract ByteBufferChunk newChunk(int bytes);
}
