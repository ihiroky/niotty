package net.ihiroky.niotty.nio;

/**
 * Factory class to create {@link WriteQueue}.
 */
public interface WriteQueueFactory<T extends WriteQueue> {
    /**
     * Instantiates a new {@link WriteQueue}.
     * @return the new {@code WriteQueue}
     */
    T newWriteQueue();
}
