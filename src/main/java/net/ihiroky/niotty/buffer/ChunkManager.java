package net.ihiroky.niotty.buffer;

/**
 * @author Hiroki Itoh
 */
public interface ChunkManager<E> extends AutoCloseable {

    Chunk<E> newChunk(int bytes);
    void release(Chunk<E> chunk);
    void close();
}
