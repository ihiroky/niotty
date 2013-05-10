package net.ihiroky.niotty.buffer;

/**
 * @author Hiroki Itoh
 */
public abstract class ChunkManager<E> implements AutoCloseable {

    public abstract Chunk<E> newChunk(int bytes);
    protected abstract void release(Chunk<E> chunk);
    public abstract void close();
}
