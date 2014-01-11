package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.buffer.Packet;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

/**
 *
 */
public class SimpleDatagramQueue implements DatagramQueue {

    private Queue<AttachedMessage<Packet>> queue_;
    private volatile int size_; // broken if queue_ has elements more than Integer.MAX_VALUE.

    private static final AtomicIntegerFieldUpdater<SimpleDatagramQueue> SIZE_UPDATER =
            AtomicIntegerFieldUpdater.newUpdater(SimpleDatagramQueue.class, "size_");

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

    public SimpleDatagramQueue() {
        queue_ = new ConcurrentLinkedQueue<AttachedMessage<Packet>>();
    }

    @Override
    public boolean offer(AttachedMessage<Packet> message) {
        incrementSize();
        return queue_.offer(message);
    }

    @Override
    public FlushStatus flush(DatagramChannel channel, ByteBuffer writeBuffer) throws IOException {
        for (;;) {
            AttachedMessage<Packet> message = queue_.peek();
            if (message == null) {
                return FlushStatus.FLUSHED;
            }

            Packet buffer = message.message();
            SocketAddress target = (SocketAddress) message.parameter();
            boolean sunk = (target != null) ? buffer.sink(channel, writeBuffer, target) : buffer.sink(channel);
            if (sunk) {
                buffer.dispose();
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
