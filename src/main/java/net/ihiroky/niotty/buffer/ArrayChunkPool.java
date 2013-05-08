package net.ihiroky.niotty.buffer;

import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

/**
 * TODO MBean support
 * @author Hiroki Itoh
 */
public class ArrayChunkPool extends ChunkPool<byte[]> {

    // TODO support long
    private volatile int allocatedBytes_;
    private final int maxPoolingBytes_;

    private static final AtomicIntegerFieldUpdater<ArrayChunkPool> ALLOCATED_BYTES_UPDATER =
            AtomicIntegerFieldUpdater.newUpdater(ArrayChunkPool.class, "allocatedBytes_");

    ArrayChunkPool(int maxPoolingBytes) {
        if (maxPoolingBytes <= 0) {
            throw new IllegalArgumentException("maxPoolingBytes must be positive.");
        }
        maxPoolingBytes_ = maxPoolingBytes;
    }

    @Override
    protected ArrayChunk allocate(int bytes) {
        for (;;) {
            int current = allocatedBytes_;
            int next = current + bytes;
            if (next > maxPoolingBytes_) {
                return new ArrayChunk(new byte[bytes], ArrayChunkFactory.INSTANCE);
            }
            if (ALLOCATED_BYTES_UPDATER.compareAndSet(this, current, next)) {
                return new ArrayChunk(new byte[bytes], this);
            }
        }
    }

    @Override
    protected void dispose() {
    }

    int allocatedBytes() {
        return allocatedBytes_;
    }
}
