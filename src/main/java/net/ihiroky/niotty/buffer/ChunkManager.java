package net.ihiroky.niotty.buffer;

/**
 * A manager to manage chunk instances.
 * This interface is used to get the chunk instance instead of creating its instance directly.
 * If the chunk is not used any more, then call {@link #release(Chunk)} with it.
 *
 * @param <E> a data type to be held by the chunk
 * @author Hiroki Itoh
 */
public abstract class ChunkManager<E> implements AutoCloseable {

    /**
     * Provides the chunk managed by this instance.
     * @param bytes a size of the chunk in byte
     * @return the chunk, which size can be larger than {@code bytes}
     */
    public abstract Chunk<E> newChunk(int bytes);

    /**
     * Retrieves the chunk, which is obtained by {@link #newChunk(int)}.
     * This method should be called by the {@link net.ihiroky.niotty.buffer.Chunk#release()} if necessary.
     *
     * @param chunk the chunk which is obtained by {@code newChunk(int)}
     */
    protected abstract void release(Chunk<E> chunk);

    /**
     * Closes this manager and releases any resources associated with the manager.
     */
    public abstract void close();
}
