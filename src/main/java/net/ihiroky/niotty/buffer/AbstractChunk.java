package net.ihiroky.niotty.buffer;

import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

/**
 * A skeletal implementation of {@link net.ihiroky.niotty.buffer.Chunk}.
 * @param <E> a type of data to be managed by this instance
 * @author Hiroki Itoh
 */
abstract class AbstractChunk<E> implements Chunk<E> {

    /** A data to be managed by this instance. */
    protected final E buffer_;

    /** A manager which manages this instance. */
    private final ChunkManager<E> manager_;

    /** The reference count. */
    private volatile int referenceCount_;

    @SuppressWarnings({ "unchecked", "raw" })
    private static final AtomicIntegerFieldUpdater<AbstractChunk> REFERENCE_COUNT_UPDATER =
            AtomicIntegerFieldUpdater.newUpdater(AbstractChunk.class, "referenceCount_");

    /** The chunk state that it is unusable. */
    static final int UNUSABLE = 0;

    /** The chunk state that it is required to call initialize before use. */
    static final int PRE_INITIALIZED = -1;

    /**
     * Constructs this chunk.
     * @param buffer a data to be managed by this instance
     * @param manager  a manager to manage this instance
     */
    @SuppressWarnings("unchecked")
    AbstractChunk(E buffer, ChunkManager<E> manager) {
        buffer_ = buffer;
        manager_ = manager;
        referenceCount_ = UNUSABLE;
    }

    @Override
    public E initialize() {
        if (!REFERENCE_COUNT_UPDATER.compareAndSet(this, PRE_INITIALIZED, 1)) {
            throw new IllegalStateException("this chunk is not in the pre-initialized state.");
        }
        referenceCount_ = 1;
        return buffer_;
    }

    /**
     * Increments the reference count by 1.
     * @return the incremented reference count.
     */
    int incrementReferenceCount() {
        for (;;) {
            int current = referenceCount_;
            if (current <= 0) {
                throw new IllegalStateException("this chunk is already released or not initialized yet.");
            }
            int next = current + 1;
            if (REFERENCE_COUNT_UPDATER.compareAndSet(this, current, next)) {
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
            if (REFERENCE_COUNT_UPDATER.compareAndSet(this, current, next)) {
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
        if (!REFERENCE_COUNT_UPDATER.compareAndSet(this, UNUSABLE, PRE_INITIALIZED)) {
            throw new IllegalStateException("this chunk is not in the unusable state.");
        }
        referenceCount_ = PRE_INITIALIZED;
    }

    @Override
    public ChunkManager<E> manager() {
        return manager_;
    }
}
