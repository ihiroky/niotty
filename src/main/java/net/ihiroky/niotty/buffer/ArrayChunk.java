package net.ihiroky.niotty.buffer;

/**
* @author Hiroki Itoh
*/
public class ArrayChunk extends AbstractChunk<byte[]> {

    public ArrayChunk(byte[] buffer, ChunkManager<byte[]> allocator) {
        super(buffer, allocator);
    }

    @Override
    public byte[] retain() {
        incrementRetainCount();
        return buffer_;
    }

    @Override
    public int size() {
        return buffer_.length;
    }
}
