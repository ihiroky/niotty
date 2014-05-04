package net.ihiroky.niotty.nio;

/**
 * A write queue factory, which is used in {@link net.ihiroky.niotty.nio.NioClientSocketTransport}.
 */
public class SimplePacketQueueFactory implements WriteQueueFactory<PacketQueue> {

    private final int queueCapacity_;

    public SimplePacketQueueFactory() {
        this(0);
    }

    /**
     * Creates a new instance.
     * @param queueCapacity the capacity of the queue, negative or 0 if use unbounded queue
     */
    public SimplePacketQueueFactory(int queueCapacity) {
        queueCapacity_ = queueCapacity;
    }

    @Override
    public SimplePacketQueue newWriteQueue() {
        return new SimplePacketQueue(queueCapacity_);
    }
}
