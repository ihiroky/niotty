package net.ihiroky.niotty.buffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.nio.ch.DirectBuffer;

import java.nio.ByteBuffer;

/**
 * @author Hiroki Itoh
 */
public enum ByteBufferAllocator implements BufferAllocator<ByteBuffer> {

    HEAP(false),
    DIRECT(true),
    ;

    private final boolean direct_;
    private Logger logger_ = LoggerFactory.getLogger(ByteBufferAllocator.class);

    private ByteBufferAllocator(boolean direct) {
        direct_ = direct;
    }

    @Override
    public Chunk<ByteBuffer> allocate(int bytes) {
        return direct_
                ? new Chunk.ByteBufferChunk(ByteBuffer.allocateDirect(bytes), this)
                : new Chunk.ByteBufferChunk(ByteBuffer.allocate(bytes), this);
    }

    @Override
    public void release(Chunk<ByteBuffer> chunk) {
        ByteBuffer b = chunk.buffer();
        if (b.isDirect()) {
            try {
                ((DirectBuffer) b).cleaner().clean();
            } catch (Throwable t) {
                logger_.debug("[release] failed.", t);
            }
        }
    }

    @Override
    public void free() {
    }
}
