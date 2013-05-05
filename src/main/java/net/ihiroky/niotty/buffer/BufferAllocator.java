package net.ihiroky.niotty.buffer;

/**
 * @author Hiroki Itoh
 */
public interface BufferAllocator<E> {

    Chunk<E> allocate(int bytes);
    void release(Chunk<E> chunk);
    void free();
}
