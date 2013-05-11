package net.ihiroky.niotty.buffer;

import java.io.IOException;
import java.nio.channels.GatheringByteChannel;

/**
 * Writes data into a given {@code java.nio.channel.WritableByteChannel}.
 * This class has two member; beginning and end.
 * The beginning shows the start index (included) of data to be written.
 * The end shows the end index (excluded) of data to be written.
 * <p></p>
 * Add interface if new Transport is added and new data type is required.
 * @author Hiroki Itoh
 */
public interface BufferSink {
    /**
     * Writes data in this instance to the given {@code channel}.
     *
     * @param channel the {@code WritableByteChannel} to be written into
     * @return true if all data in this instance is written into the {@code channel}
     * @throws IOException if I/O operation is failed
     */
    boolean transferTo(GatheringByteChannel channel) throws IOException;

    /**
     * Adds a specified buffer before data which already exists in this instance .
     * @param buffer buffer to be added
     * @return this instance
     */
    BufferSink addFirst(CodecBuffer buffer);

    /**
     * Adds a specified buffer after data which already exists in this instance.
     * @param buffer buffer to be added
     * @return this instance
     */
    BufferSink addLast(CodecBuffer buffer);


    /**
     * Creates a new {@code BufferSink} that shares the base content.
     * The beginning of the new {@code BufferSink} is the one of the this instance.
     * The end of the new {@code BufferSink} is {@code beginning + bytes}.
     * The two {@code BufferSink}'s beginning and end are independent.
     * After this method is called, the beginning of this instance increases {@code bytes}.
     *
     * @param bytes size of content to slice
     * @throws IllegalArgumentException if {@code bytes} exceeds this buffer's remaining.
     * @return the new {@code BufferSink}
     */
    BufferSink slice(int bytes);

    /**
     * Creates a new {@code BufferSink} that shares the base content.
     * The content of the new buffer will be that of this buffer. Changes to this buffer's content will be visible
     * in the new buffer, and vice versa; the two {@code BufferSink}s' beginning and end values will be independent.
     * The new buffer's beginning and end values will be identical to those of this buffer.
     *
     * @return the new {@code BufferSink}
     */
    BufferSink duplicate();

    /**
     * The byte size of remaining data in this instance.
     * @return the byte size of remaining data in this instance.
     */
    int remainingBytes();

    /**
     * The value used by {@link net.ihiroky.niotty.nio.WriteQueue}.
     * @return priority
     */
    int priority();

    /**
     * Disposes resources managed in this class if exists.
     */
    void dispose();
}
