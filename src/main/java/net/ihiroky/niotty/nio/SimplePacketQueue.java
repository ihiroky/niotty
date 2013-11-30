package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.buffer.BufferSink;

import java.io.IOException;
import java.nio.channels.GatheringByteChannel;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 *
 */
public class SimplePacketQueue implements PacketQueue {

    private Queue<BufferSink> queue_;

    public SimplePacketQueue() {
        queue_ = new ConcurrentLinkedQueue<BufferSink>();
    }

    @Override
    public boolean offer(BufferSink bufferSink) {
        return queue_.offer(bufferSink);
    }

    @Override
    public FlushStatus flush(GatheringByteChannel channel) throws IOException {
        for (;;) {
            BufferSink message = queue_.peek();
            if (message == null) {
                return FlushStatus.FLUSHED;
            }

            if (message.sink(channel)) {
                message.dispose();
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
