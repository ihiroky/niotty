package net.ihiroky.niotty.nio;

/**
 * Factory class to create {@link net.ihiroky.niotty.nio.WriteQueue}.
 * @author Hiroki Itoh
 */
public interface WriteQueueFactory {
    /**
     * Instantiates a new {@link net.ihiroky.niotty.nio.WriteQueue}.
     * @return the new {@code WriteQueue}
     */
    WriteQueue newWriteQueue();
}
