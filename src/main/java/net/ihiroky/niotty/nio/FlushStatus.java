package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.Event;

/**
 * Shows a result of
 * {@link net.ihiroky.niotty.nio.WriteQueue#flushTo(java.nio.channels.GatheringByteChannel)}
 * and {@link net.ihiroky.niotty.nio.WriteQueue#flushTo(java.nio.channels.DatagramChannel, java.nio.ByteBuffer)}.
 */
public enum FlushStatus {
    /**
     * The result of all data in the {@code WriteQueue} is flushed.
     */
    FLUSHED(Event.DONE),

    /**
     * The result of all data in the {@code WriteQueue} is not flushed.
     */
    FLUSHING(100),

    /**
     * The result of data in the {@code WriteQueue} is remaining but not flushed because of some limitation.
     */
    SKIPPED(Event.RETRY_IMMEDIATELY);

    /** a wait time for I/O round operation. */
    final long waitTimeMillis_;

    FlushStatus(long waitTimeMillis) {
        waitTimeMillis_ = waitTimeMillis;
    }
}
