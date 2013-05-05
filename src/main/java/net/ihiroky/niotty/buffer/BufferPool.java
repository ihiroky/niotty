package net.ihiroky.niotty.buffer;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author Hiroki Itoh
 */
public abstract class BufferPool<E> implements BufferAllocator<E> {

    private Queue<Chunk<E>>[] buffers_;
    private final int maxChunkBytes_;
    private final boolean aggressive_;

    // 0/00  2    4    8
    // count 500  250  125
    // 64M   128k 256k 512k
    // 512M  1M   2M   4M
    // 1G    2M   4M   8M
    protected static final int K = 1024;
    protected static final int MAX_CHUNK_PER_MILL = 8;

    @SuppressWarnings("unchecked")
    private static <E> Queue<E>[] newArray(int size) {
        return (Queue<E>[]) new Queue<?>[size];
    }

    BufferPool(int wholeBytes, int maxChunkBytes, boolean aggressive) {
        if (wholeBytes <= 0) {
            throw new IllegalArgumentException("wholeBytes must be positive.");
        }
        if (maxChunkBytes <= 0) {
            throw new IllegalArgumentException("maxChunkBytes must be positive.");
        }
        if (wholeBytes < maxChunkBytes) {
            throw new IllegalArgumentException("wholeBytes must be greater than or equal " + MAX_CHUNK_PER_MILL + ".");
        }
        buffers_ = newArray(Integer.SIZE);
        for (int i = 0; i < Integer.SIZE; i++) {
            buffers_[i] = new ConcurrentLinkedQueue<>();
        }
        maxChunkBytes_ = maxChunkBytes;
        aggressive_ = aggressive;
    }

    protected int maxChunkBytes() {
        return maxChunkBytes_;
    }

    @Override
    public Chunk<E> allocate(int bytes) {
        int normalizedBytes = powerOfTwoGreaterThanOrEquals(bytes);
        int queue = Integer.numberOfTrailingZeros(normalizedBytes);
        Queue<Chunk<E>> pool = buffers_[queue];
        Chunk<E> chunk = pool.poll();
        if (chunk == null) {
            chunk = allocate(normalizedBytes, aggressive_);
        }
        return chunk;
    }

    @Override
    public void release(Chunk<E> chunk) {
        int queue = Integer.numberOfTrailingZeros(chunk.size());
        buffers_[queue].offer(chunk);
    }

    @Override
    public void free() {
        dispose();
        for (Queue<Chunk<E>> queue : buffers_) {
            queue.clear();
        }
    }

    abstract Chunk<E> allocate(int bytes, boolean aggressive);
    abstract void dispose();

    private static int powerOfTwoGreaterThanOrEquals(int bytes) {
        return (bytes > 1) ? Integer.highestOneBit(bytes - 1) << 1 : 1;
    }
}
