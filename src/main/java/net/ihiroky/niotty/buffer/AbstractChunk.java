package net.ihiroky.niotty.buffer;

import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

/**
 * @author Hiroki Itoh
 */
abstract class AbstractChunk<E> implements Chunk<E> {

    protected final E buffer_;
    private final ChunkManager<E> manager_;

    private volatile int retainCount_;

    @SuppressWarnings({ "unchecked", "raw" })
    private static final AtomicIntegerFieldUpdater<AbstractChunk> RETAIN_COUNT_UPDATER =
            AtomicIntegerFieldUpdater.newUpdater(AbstractChunk.class, "retainCount_");

    private static final ChunkManager<?> NULL_MANAGER = new ChunkManager<Object>() {
        @Override
        public Chunk<Object> newChunk(int bytes) {
            throw new UnsupportedOperationException("I am null allocator.");
        }
        @Override
        public void release(Chunk<Object> chunk) {
        }
        @Override
        public void close() {
        }
    };

    @SuppressWarnings("unchecked")
    AbstractChunk(E buffer, ChunkManager<E> manager) {
        buffer_ = buffer;
        manager_ = (manager != null) ? manager : (ChunkManager<E>) NULL_MANAGER;
    }

    @Override
    public E initialize() {
        retainCount_ = 1;
        return buffer_;
    }

    int incrementRetainCount() {
        for (;;) {
            int current = retainCount_;
            if (current <= 0) {
                throw new IllegalStateException("this chunk is already released or not initialized yet.");
            }
            int next = current + 1;
            if (RETAIN_COUNT_UPDATER.compareAndSet(this, current, next)) {
                return next;
            }
        }
    }

    @Override
    public int release() {
        for (;;) {
            int current = retainCount_;
            if (current <= 0) {
                throw new IllegalStateException("this chunk is already released or not initialized yet.");
            }
            int next = current - 1;
            if (RETAIN_COUNT_UPDATER.compareAndSet(this, current, next)) {
                if (next == 0) {
                    manager_.release(this);
                }
                return next;
            }
        }
    }

    @Override
    public Chunk<E> reallocate(int bytes) {
        Chunk<E> newChunk = manager_.newChunk(bytes);
        release();
        return newChunk;
    }

    @Override
    public int retainCount() {
        return retainCount_;
    }

    ChunkManager<E> manager() {
        return manager_;
    }
}
