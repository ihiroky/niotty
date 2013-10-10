package net.ihiroky.niotty.buffer;

import sun.nio.ch.DirectBuffer;

import java.nio.ByteBuffer;

/**
 * A implementation of {@link Chunk} that holds an {@code ByteBuffer}.
 * @author Hiroki Itoh
 */
public class ByteBufferChunk extends AbstractChunk<ByteBuffer> {

    /**
     * Constructs this instance.
     * @param buffer the byte array to be managed by this instance
     * @param manager the manager to manage this instance
     */
    public ByteBufferChunk(ByteBuffer buffer, ChunkManager<ByteBuffer> manager) {
        super(buffer, manager);
    }

    @Override
    public ByteBuffer retain() {
        incrementReferenceCount();
        return buffer_.duplicate();
    }

    @Override
    public int size() {
        return buffer_.capacity();
    }

    /**
     * Clears the {@code ByteBuffer} which contains in this chunk if the {@code ByteBuffer} is the direct buffer.
     * @throws Throwable
     */
    void clear() throws Throwable {
        if (buffer_ instanceof DirectBuffer) {
            ((DirectBuffer) buffer_).cleaner().clean();
        }
    }
}
