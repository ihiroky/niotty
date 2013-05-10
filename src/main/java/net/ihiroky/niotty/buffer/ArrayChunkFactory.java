package net.ihiroky.niotty.buffer;

/**
 * @author Hiroki Itoh
 */
public class ArrayChunkFactory extends ChunkManager<byte[]> {

    private static final ArrayChunkFactory INSTANCE = new ArrayChunkFactory();

    private ArrayChunkFactory() {
    }

    public static ArrayChunkFactory instance() {
        return INSTANCE;
    }

    @Override
    public Chunk<byte[]> newChunk(int bytes) {
        ArrayChunk c = new ArrayChunk(new byte[bytes], this);
        c.ready();
        return c;
    }

    @Override
    protected void release(Chunk<byte[]> chunk) {
    }

    @Override
    public void close() {
    }
}
