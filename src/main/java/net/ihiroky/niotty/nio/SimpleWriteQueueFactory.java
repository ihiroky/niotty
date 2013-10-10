package net.ihiroky.niotty.nio;

/**
 * Implementation of {@link WriteQueueFactory}
 * to create {@link SimpleWriteQueue}.
 * @author Hiroki Itoh
 */
public class SimpleWriteQueueFactory implements WriteQueueFactory {

    /**
     * {@inheritDoc}
     */
    @Override
    public WriteQueue newWriteQueue() {
        return new SimpleWriteQueue();
    }
}
