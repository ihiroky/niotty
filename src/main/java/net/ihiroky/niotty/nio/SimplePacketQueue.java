package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.buffer.Packet;

import java.io.IOException;
import java.nio.channels.GatheringByteChannel;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 *
 */
public class SimplePacketQueue implements PacketQueue {

    private Queue<Packet> queue_;

    public SimplePacketQueue() {
        queue_ = new ConcurrentLinkedQueue<Packet>();
    }

    @Override
    public boolean offer(Packet packet) {
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
