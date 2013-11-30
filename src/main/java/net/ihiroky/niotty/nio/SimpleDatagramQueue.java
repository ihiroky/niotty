package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.buffer.BufferSink;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 *
 */
public class SimpleDatagramQueue implements DatagramQueue {

    private Queue<AttachedMessage<BufferSink>> queue_;

    public SimpleDatagramQueue() {
        queue_ = new ConcurrentLinkedQueue<AttachedMessage<BufferSink>>();
    }

    @Override
    public boolean offer(AttachedMessage<BufferSink> message) {
        return queue_.offer(message);
    }

    @Override
    public FlushStatus flush(DatagramChannel channel, ByteBuffer writeBuffer) throws IOException {
        for (;;) {
            AttachedMessage<BufferSink> message = queue_.peek();
            if (message == null) {
                return FlushStatus.FLUSHED;
            }

            BufferSink buffer = message.message();
            SocketAddress target = (SocketAddress) message.parameter();
            boolean sunk = (target != null) ? buffer.sink(channel, writeBuffer, target) : buffer.sink(channel);
            if (sunk) {
                buffer.dispose();
                queue_.poll();
            } else {
                return FlushStatus.FLUSHING;
            }
        }
    }

    @Override
    public int size() {
        return queue_.size();
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
