package net.ihiroky.niotty.nio;

/**
 *
 */
public class SimplePacketQueueFactory implements WriteQueueFactory<PacketQueue> {
    @Override
    public SimplePacketQueue newWriteQueue() {
        return new SimplePacketQueue();
    }
}
