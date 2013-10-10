package net.ihiroky.niotty.buffer;

/**
 * A factory class that creates {@link ArrayChunk}.
 * Use {@link #instance()} to get an instance of this class.
 *
 * @author Hiroki Itoh
 */
public class ArrayChunkFactory extends ChunkManager<byte[]> {

    private static final ArrayChunkFactory INSTANCE = new ArrayChunkFactory();

    private ArrayChunkFactory() {
    }

    /**
     * Returns the instance.
     * @return the instance.
     */
    public static ArrayChunkFactory instance() {
        return INSTANCE;
    }

    @Override
    protected Chunk<byte[]> newChunk(int bytes) {
        ArrayChunk c = new ArrayChunk(new byte[bytes], this);
        c.ready();
        return c;
    }

    /**
     * Does nothing.
     * @param chunk the chunk which is obtained by {@code newChunk(int)}
     */
    @Override
    protected void release(Chunk<byte[]> chunk) {
    }

    /**
     * Does nothing.
     */
    @Override
    public void close() {
    }
}
