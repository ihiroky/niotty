package net.ihiroky.niotty.buffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.nio.ch.DirectBuffer;

import java.nio.ByteBuffer;

/**
 * @author Hiroki Itoh
 */
public class ByteBufferPool extends BufferPool<ByteBuffer> {

    private final ByteBuffer whole_;
    private Logger logger_ = LoggerFactory.getLogger(ByteBufferPool.class);

    ByteBufferPool(int wholeBytes, int maxChunkBytes, boolean direct, boolean aggressive) {
        super(wholeBytes, maxChunkBytes, aggressive);
        whole_ = direct ? ByteBuffer.allocateDirect(wholeBytes) : ByteBuffer.allocate(wholeBytes);
    }

    private ByteBuffer newBuffer(int bytes, boolean direct) {
        return direct ? ByteBuffer.allocateDirect(bytes) : ByteBuffer.allocate(bytes);
    }

    @Override
    Chunk<ByteBuffer> allocate(int bytes, boolean aggressive) {
        if (bytes > maxChunkBytes()) {
            return aggressive
                    ? new Chunk.ByteBufferChunk(newBuffer(bytes, whole_.isDirect()), this)
                    : new Chunk.ByteBufferChunk(newBuffer(bytes, false));
        }

        Chunk<ByteBuffer> chunk;
        synchronized (whole_) {
            if (bytes >= whole_.remaining()) {
                chunk = aggressive
                        ? new Chunk.ByteBufferChunk(newBuffer(bytes, whole_.isDirect()), this)
                        : new Chunk.ByteBufferChunk(newBuffer(bytes, false));
            } else {
                int limit = whole_.position() + bytes;
                whole_.limit(limit);
                ByteBuffer sliced = whole_.slice();
                whole_.position(limit);
                chunk = new Chunk.ByteBufferChunk(sliced, this);
            }
        }
        return chunk;
    }

    @Override
    void dispose() {
        try {
            if (whole_.isDirect()) {
                ((DirectBuffer) whole_).cleaner().clean();
            }
        } catch (Throwable t) {
            logger_.debug("[dispose] failed.", t);
        }
    }
}
