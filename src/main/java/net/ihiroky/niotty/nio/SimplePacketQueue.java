package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.buffer.Packet;
import net.ihiroky.niotty.util.MPSCArrayQueue;

import java.io.IOException;
import java.nio.channels.GatheringByteChannel;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

/**
 *
 */
public class SimplePacketQueue implements PacketQueue {

    private Queue<Packet> queue_;

    @SuppressWarnings("unused")
    private volatile int size_; // For monitoring. Broken if queue_ has elements more than Integer.MAX_VALUE.

    private static final AtomicIntegerFieldUpdater<SimplePacketQueue> SIZE_UPDATER =
            AtomicIntegerFieldUpdater.newUpdater(SimplePacketQueue.class, "size_");

    private void incrementSize() {
        int size;
        for (;;) {
            size = size_;
            if (SIZE_UPDATER.compareAndSet(this, size, size + 1)) {
                return;
            }
        }
    }

    private void decrementSize() {
        int size;
        for (;;) {
            size = size_;
            if (SIZE_UPDATER.compareAndSet(this, size, size - 1)) {
                return;
            }
        }
    }

    /**
     * Creates a new instance.
     * @param queueCapacity the capacity of the queue, negative or 0 if use unbounded queue
     */
    public SimplePacketQueue(int queueCapacity) {
        queue_ = (queueCapacity <= 0)
                ? new ConcurrentLinkedQueue<Packet>()
                : new MPSCArrayQueue<Packet>(queueCapacity);
    }

    @Override
    public boolean offer(Packet packet) {
        incrementSize();
        return queue_.offer(packet);
    }

    @Override
    public FlushStatus flush(GatheringByteChannel channel) throws IOException {
        for (;;) {
            Packet message = queue_.peek();
            if (message == null) {
                return FlushStatus.FLUSHED;
            }

            if (message.sink(channel)) {
                message.dispose();
                queue_.poll();
                decrementSize();
            } else {
                return FlushStatus.FLUSHING;
            }
        }
    }

    @Override
    public int size() {
        return size_;
    }

    @Override
    public boolean isEmpty() {
        return queue_.isEmpty();
    }

    @Override
    public void clear() {
        queue_.clear();
    }
}
