package net.ihiroky.niotty.buffer;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * An {@link net.ihiroky.niotty.buffer.ChunkManager} that holds available {@link net.ihiroky.niotty.buffer.Chunk}s
 * in a pool and provides it as necessary from the pool.
 * <p></p>
 * Call {@link #newChunk(int)} to obtain an available chunk. If the chunk is finish using,
 * then call {@link #release(Chunk)} to return the chunk. The pool can have size limit to allocate chunks.
 * {@code ChunkPool} may provides unpooled chunks which is not managed by the {@code ChunkPool}
 * when the size breaks the limit.
 * <p></p>
 * The capacity size of chunks is a minimum power of tow that is larger than the specified value
 * in {@link #newChunk(int)}.
 *
 * @param <E> the content type in the chunk
 * @author Hiroki Itoh
 */
public abstract class ChunkPool<E> extends ChunkManager<E> {

    /** The pool to hold available chunks. */
    private Queue<Chunk<E>>[] pools_;

    /**
     * A reference count of chunks.
     * This is incremented by one when a chunk is obtained from outside and decremented by one when brought back.
     */
    private AtomicInteger referredChunkCount_;

    @SuppressWarnings("unchecked")
    private static <E> Queue<E>[] newArray(int size) {
        return (Queue<E>[]) new Queue<?>[size];
    }

    ChunkPool() {
        pools_ = newArray(Integer.SIZE);
        for (int i = 0; i < Integer.SIZE; i++) {
            pools_[i] = new ConcurrentLinkedQueue<>();
        }
        referredChunkCount_ = new AtomicInteger();
    }

    /**
     * Returns the chunk from the pool. The chunk may be unppoled when the size limit exists.
     * @param bytes a size of the chunk in byte.
     * @return the chunk.
     */
    @Override
    public Chunk<E> newChunk(int bytes) {
        int normalizedBytes = powerOfTwoGreaterThanOrEquals(bytes);
        int queue = Integer.numberOfTrailingZeros(normalizedBytes);
        Queue<Chunk<E>> pool = pools_[queue];
        Chunk<E> chunk = pool.poll();
        if (chunk == null) {
            chunk = allocate(normalizedBytes);
        }
        chunk.ready();
        if (chunk.manager() == this) {
            referredChunkCount_.incrementAndGet();
        }
        return chunk;
    }

    /**
     * {@inheritDoc}
     * @param chunk the chunk which is obtained by {@code newChunk(int)}.
     * @throws IllegalArgumentException if the chunk doesn't belong to this instance.
     */
    @Override
    protected void release(Chunk<E> chunk) {
        if (chunk.manager() != this) {
            throw new IllegalArgumentException("The chunk doesn't belong to this ChunkPool.");
        }
        int queue = Integer.numberOfTrailingZeros(chunk.size());
        pools_[queue].offer(chunk);
        referredChunkCount_.decrementAndGet();
    }

    /**
     * {@inheritDoc}
     * @throws IllegalStateException if all the chunks is not brought back (released) to this instance.
     */
    @Override
    public void close() {
        if (referredChunkCount_.get() != 0) {
            throw new IllegalStateException(referredChunkCount_.get() + " chunks are still in use.");
        }
        dispose();
        for (Queue<Chunk<E>> queue : pools_) {
            queue.clear();
        }
    }

    Queue<Chunk<E>>[] pools() {
        Queue<Chunk<E>>[] copy = newArray(pools_.length);
        for (int i = 0; i < copy.length; i++) {
            copy[i] = new ArrayDeque<>(pools_[i]);
        }
        return copy;
    }

    /**
     * Returns a reference count of chunks from outside of this instance.
     * @return a reference count of chunks from outside of this instance.
     */
    public int referredChunkCount() {
        return referredChunkCount_.get();
    }

    /**
     * Allocates a new chunk.
     * @param bytes a size of the chunk.
     * @return the new chunk.
     */
    protected abstract Chunk<E> allocate(int bytes);

    /**
     * Disposes managed resources if necessary.
     */
    protected abstract void dispose();

    private static int powerOfTwoGreaterThanOrEquals(int bytes) {
        return (bytes > 1) ? Integer.highestOneBit(bytes - 1) << 1 : 1;
    }
}
