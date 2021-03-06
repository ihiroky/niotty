package net.ihiroky.niotty.buffer;

import net.ihiroky.niotty.util.Arguments;

import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

/**
 * A implementation of {@link ChunkPool} which manages
 * {@link ArrayChunk}. This class has maximum size to allocate the chunks.
 * If total size of the pooled chunks exceeds the size, unpooled chunks are allocated.
 * <p></p>
 * TODO MBean support
 * @author Hiroki Itoh
 */
public class ArrayChunkPool extends ChunkPool<byte[]> {

    /**
     * The total size of allocated chunks.
     * TODO support long value.
     */
    private volatile int allocatedBytes_;

    /**
     * The maximum total size of chunks which this pool can hold.
     */
    private final int maxPoolingBytes_;

    private static final AtomicIntegerFieldUpdater<ArrayChunkPool> ALLOCATED_BYTES_UPDATER =
            AtomicIntegerFieldUpdater.newUpdater(ArrayChunkPool.class, "allocatedBytes_");

    /**
     * Constructs a new instance.
     * @param maxPoolingBytes the maxPoolingBytes.
     * @throws IllegalArgumentException if the maxPoolingBytes is not positive integer.
     */
    public ArrayChunkPool(int maxPoolingBytes) {
        maxPoolingBytes_ = Arguments.requirePositive(maxPoolingBytes, "maxPoolingBytes");
    }

    @Override
    protected ArrayChunk allocate(int bytes) {
        for (;;) {
            int current = allocatedBytes_;
            int next = current + bytes;
            if (next > maxPoolingBytes_) {
                return new ArrayChunk(new byte[bytes], ArrayChunkFactory.instance());
            }
            if (ALLOCATED_BYTES_UPDATER.compareAndSet(this, current, next)) {
                return new ArrayChunk(new byte[bytes], this);
            }
        }
    }

    /**
     * Does nothing.
     */
    @Override
    protected void dispose() {
    }

    int allocatedBytes() {
        return allocatedBytes_;
    }
}
