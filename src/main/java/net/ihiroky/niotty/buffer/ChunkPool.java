package net.ihiroky.niotty.buffer;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 *
 * @author Hiroki Itoh
 */
public abstract class ChunkPool<E> implements ChunkManager<E>, AutoCloseable {

    private Queue<Chunk<E>>[] pools_;

    @SuppressWarnings("unchecked")
    private static <E> Queue<E>[] newArray(int size) {
        return (Queue<E>[]) new Queue<?>[size];
    }

    ChunkPool() {
        pools_ = newArray(Integer.SIZE);
        for (int i = 0; i < Integer.SIZE; i++) {
            pools_[i] = new ConcurrentLinkedQueue<>();
        }
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
        return chunk;
    }

    @Override
    public void release(Chunk<E> chunk) {
        int queue = Integer.numberOfTrailingZeros(chunk.size());
        pools_[queue].offer(chunk);
    }

    @Override
    public void close() {
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

    protected abstract Chunk<E> allocate(int bytes);
    protected abstract void dispose();

    private static int powerOfTwoGreaterThanOrEquals(int bytes) {
        return (bytes > 1) ? Integer.highestOneBit(bytes - 1) << 1 : 1;
    }
}
