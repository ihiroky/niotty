package net.ihiroky.niotty.nio;

/**
 * Factory class to create {@link WriteQueue}.
 */
public interface WriteQueueFactory {
    /**
     * Instantiates a new {@link WriteQueue}.
     * @return the new {@code WriteQueue}
     */
    WriteQueue newWriteQueue();
}
