package net.ihiroky.niotty.buffer;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * @author Hiroki Itoh
 */
public abstract class ChunkPool<E> extends ChunkManager<E> {

    private Queue<Chunk<E>>[] pools_;
    private AtomicInteger usedChunkCount_;

    @SuppressWarnings("unchecked")
    private static <E> Queue<E>[] newArray(int size) {
        return (Queue<E>[]) new Queue<?>[size];
    }

    ChunkPool() {
        pools_ = newArray(Integer.SIZE);
        for (int i = 0; i < Integer.SIZE; i++) {
            pools_[i] = new ConcurrentLinkedQueue<>();
        }
        usedChunkCount_ = new AtomicInteger();
    }

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
            usedChunkCount_.incrementAndGet();
        }
        return chunk;
    }

    @Override
    protected void release(Chunk<E> chunk) {
        int queue = Integer.numberOfTrailingZeros(chunk.size());
        pools_[queue].offer(chunk);
        usedChunkCount_.decrementAndGet();
    }

    @Override
    public void close() {
        if (usedChunkCount_.get() != 0) {
            throw new IllegalStateException(usedChunkCount_.get() + " chunks are already in use.");
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

    public int usedChunkCount() {
        return usedChunkCount_.get();
    }

    protected abstract Chunk<E> allocate(int bytes);
    protected abstract void dispose();

    private static int powerOfTwoGreaterThanOrEquals(int bytes) {
        return (bytes > 1) ? Integer.highestOneBit(bytes - 1) << 1 : 1;
    }
}
