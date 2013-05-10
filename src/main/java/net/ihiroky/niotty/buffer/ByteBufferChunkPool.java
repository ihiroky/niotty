package net.ihiroky.niotty.buffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.nio.ch.DirectBuffer;

import java.nio.ByteBuffer;

/**
 * TODO MBean support
 * @author Hiroki Itoh
 */
public class ByteBufferChunkPool extends ChunkPool<ByteBuffer> {

    // TODO support long wide (multiple ByteBuffer)
    private final ByteBuffer whole_;
    private Logger logger_ = LoggerFactory.getLogger(ByteBufferChunkPool.class);

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
