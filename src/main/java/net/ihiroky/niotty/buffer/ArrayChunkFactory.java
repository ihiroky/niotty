package net.ihiroky.niotty.buffer;

/**
 * @author Hiroki Itoh
 */
public enum ArrayChunkFactory implements ChunkManager<byte[]> {

    INSTANCE;

    @Override
    public Chunk<byte[]> newChunk(int bytes) {
        return new ArrayChunk(new byte[bytes], this);
    }

    @Override
    public void release(Chunk<byte[]> chunk) {
    }

    @Override
    public void close() {
    }
}
