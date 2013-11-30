package net.ihiroky.niotty.nio;

/**
 *
 */
public class SimpleDatagramQueueFactory implements WriteQueueFactory<DatagramQueue> {
    @Override
    public DatagramQueue newWriteQueue() {
        return new SimpleDatagramQueue();
    }
}
