package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.TaskLoop;
import net.ihiroky.niotty.buffer.BufferSink;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;

/**
 * A queue to holds {@link net.ihiroky.niotty.buffer.BufferSink} to be written to a channel.
 * @author Hiroki Itoh
 */
public interface WriteQueue {
    /**
     * Inserts a specified {@code bufferSink} at the tail of this queue.
     * @param bufferSink the element to add
     * @return true if the {@code bufferSink} is added to this queue.
     */
    boolean offer(BufferSink bufferSink);

    /**
     * Flushes queued {@code BufferSink}s to a specified {@code channel} using {@code writeBuffer}.
     * The {@code writeBuffer} must be cleared on returning from this method.
     * @param channel the channel to write into
     * @param writeBuffer write buffer
     * @return flush status
     * @throws IOException if I/O error occurs
     * @see {@link net.ihiroky.niotty.nio.WriteQueue.FlushStatus}
     */
    FlushStatus flushTo(WritableByteChannel channel, ByteBuffer writeBuffer) throws IOException;

    /**
     * Returns the number of elements in this queue.
     * @return the number of elements in this queue
     */
    int size();

    /**
     * Returns true if this queue contains no elements.
     * @return true if this queue contains no elements
     */
    boolean isEmpty();

    /**
     * Returns byte length of data flushed lastly by
     * {@link #flushTo(java.nio.channels.WritableByteChannel, java.nio.ByteBuffer)}.
     * @return byte length of data flushed lastly
     */
    int lastFlushedBytes();

    /**
     * Shows a result of
     * {@link net.ihiroky.niotty.nio.WriteQueue#flushTo(java.nio.channels.WritableByteChannel, java.nio.ByteBuffer)}.
     */
    enum FlushStatus {
        /**
         * The result of all data in the {@code WriteQueue} is flushed.
         */
        FLUSHED(TaskLoop.TIMEOUT_NO_LIMIT),

        /**
         * The result of all data in the {@code WriteQueue} is not flushed.
         */
        FLUSHING(100),

        /**
         * The result of data in the {@code WriteQueue} is remaining but not flushed because of some limitation.
         */
        SKIP(TaskLoop.TIMEOUT_NOW);

        /** a wait time for I/O round operation. */
        final int waitTimeMillis_;

        private FlushStatus(int waitTimeMillis) {
            waitTimeMillis_ = waitTimeMillis;
        }
    }
}
