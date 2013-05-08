package net.ihiroky.niotty.buffer;

import sun.nio.ch.DirectBuffer;

import java.nio.ByteBuffer;

/**
* @author Hiroki Itoh
*/
public class ByteBufferChunk extends AbstractChunk<ByteBuffer> {

    public ByteBufferChunk(ByteBuffer buffer, ChunkManager<ByteBuffer> allocator) {
        super(buffer, allocator);
    }

    @Override
    public ByteBuffer retain() {
        incrementRetainCount();
        return buffer_.duplicate();
    }

    @Override
    public int size() {
        return buffer_.capacity();
    }

    void clear() throws Throwable {
        if (buffer_ instanceof DirectBuffer) {
            ((DirectBuffer) buffer_).cleaner().clean();
        }
    }
}
