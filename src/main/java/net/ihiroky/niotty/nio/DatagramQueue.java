package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.buffer.BufferSink;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

/**
 *
 */
public interface DatagramQueue extends WriteQueue {

    /**
     * Inserts a specified {@code bufferSink} at the tail of this queue.
     * @param message the element to add
     * @return true if the {@code bufferSink} is added to this queue.
     */
    boolean offer(AttachedMessage<BufferSink> message);

    /**
     * Flushes queued {@code BufferSink}s to a specified {@code channel} using {@code writeBuffer}.
     * The {@code writeBuffer} is cleared on returning from this method.
     * @param channel the channel to write into
     * @param writeBuffer write buffer
     * @return flush status
     * @throws IOException if I/O error occurs
     * @throws java.nio.BufferOverflowException if the {@code writeBuffer} overflows.
     * @see FlushStatus
     */
    FlushStatus flush(DatagramChannel channel, ByteBuffer writeBuffer) throws IOException;
}
