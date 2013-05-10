package net.ihiroky.niotty.buffer;

import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

/**
 * @author Hiroki Itoh
 */
abstract class AbstractChunk<E> implements Chunk<E> {

    protected final E buffer_;
    private final ChunkManager<E> manager_;

    private volatile int referenceCount_;

    @SuppressWarnings({ "unchecked", "raw" })
    private static final AtomicIntegerFieldUpdater<AbstractChunk> RETAIN_COUNT_UPDATER =
            AtomicIntegerFieldUpdater.newUpdater(AbstractChunk.class, "referenceCount_");

    static final int UNUSABLE = 0;
    static final int PRE_INITIALIZED = -1;

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
        referenceCount_ = UNUSABLE;
    }

    @Override
    public E initialize() {
        if (!RETAIN_COUNT_UPDATER.compareAndSet(this, PRE_INITIALIZED, 1)) {
            throw new IllegalStateException("this chunk is not in the pre-initialized state.");
        }
        referenceCount_ = 1;
        return buffer_;
    }

    int incrementRetainCount() {
        for (;;) {
            int current = referenceCount_;
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
            int current = referenceCount_;
            if (current <= 0) {
                throw new IllegalStateException("this chunk is already released or not initialized yet.");
            }
            int next = current - 1;
            if (RETAIN_COUNT_UPDATER.compareAndSet(this, current, next)) {
                if (next == UNUSABLE) {
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
    public int referenceCount() {
        return referenceCount_;
    }

    @Override
    public void ready() {
        if (!RETAIN_COUNT_UPDATER.compareAndSet(this, UNUSABLE, PRE_INITIALIZED)) {
            throw new IllegalStateException("this chunk is not in the unusable state.");
        }
        referenceCount_ = PRE_INITIALIZED;
    }

    @Override
    public ChunkManager<E> manager() {
        return manager_;
    }
}
