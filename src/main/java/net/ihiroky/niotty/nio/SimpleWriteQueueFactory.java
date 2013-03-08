package net.ihiroky.niotty.nio;

/**
 * Implementation of {@link net.ihiroky.niotty.nio.WriteQueueFactory}
 * to create {@link net.ihiroky.niotty.nio.SimpleWriteQueue}.
 * @author Hiroki Itoh
 */
public class SimpleWriteQueueFactory implements WriteQueueFactory {

    /**
     * {@inheritDoc}
     */
    @Override
    public WriteQueue newriteQueue() {
        return new SimpleWriteQueue();
    }
}
