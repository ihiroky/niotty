package net.ihiroky.niotty.nio;

/**
 * A write queue factory, which is used in {@link net.ihiroky.niotty.nio.NioDatagramSocketTransport}.
 */
public class SimpleDatagramQueueFactory implements WriteQueueFactory<DatagramQueue> {

    private final int queueCapacity_;

    public SimpleDatagramQueueFactory() {
        this(0);
    }

    /**
     * Creates a new instance.
     * @param queueCapacity the capacity of the queue, negative or 0 if use unbounded queue
     */
    public SimpleDatagramQueueFactory(int queueCapacity) {
        queueCapacity_ = queueCapacity;
    }

    @Override
    public DatagramQueue newWriteQueue() {
        return new SimpleDatagramQueue(queueCapacity_);
    }
}
