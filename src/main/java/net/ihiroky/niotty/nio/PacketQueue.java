package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.buffer.BufferSink;

import java.io.IOException;
import java.nio.channels.GatheringByteChannel;

/**
 *
 */
public interface PacketQueue extends WriteQueue {

    /**
     * Inserts a specified {@code bufferSink} at the tail of this queue.
     * @param message the element to add
     * @return true if the {@code bufferSink} is added to this queue.
     */
    boolean offer(BufferSink message);

    /**
     * Flushes queued {@code BufferSink}s to a specified {@code channel} directly.
     *
     * @param channel the channel to write into
     * @return flush status
     * @throws IOException if I/O error occurs
     * @see FlushStatus
     */
    FlushStatus flush(GatheringByteChannel channel) throws IOException;
}
