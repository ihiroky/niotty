package net.ihiroky.niotty.buffer;

/**
 * @author Hiroki Itoh
 */
public enum ByteArrayAllocator implements BufferAllocator<byte[]> {

    INSTANCE;

    @Override
    public Chunk<byte[]> allocate(int bytes) {
        return new Chunk.ByteArrayChunk(new byte[bytes], this);
    }

    @Override
    public void release(Chunk<byte[]> chunk) {
    }

    @Override
    public void free() {
    }
}
