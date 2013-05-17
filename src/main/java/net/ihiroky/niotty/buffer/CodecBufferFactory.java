package net.ihiroky.niotty.buffer;

/**
 * Provides methods to create {@link net.ihiroky.niotty.buffer.CodecBuffer}.
 *
 * @author Hiroki Itoh
 */
public interface CodecBufferFactory {

    /**
     * Creates a new {@code CodecBuffer} which has the specified space.
     *
     * @param bytes the size of the space.
     * @return the new {@code CodecBuffer}.
     */
    CodecBuffer newCodecBuffer(int bytes);

    /**
     * Creates a new {@code CodecBuffer} which has the specified space and the priority.
     * @param bytes the size of the space.
     * @param priority the priority.
     * @return the new {@code CodecBuffer}.
     */
    CodecBuffer newCodecBuffer(int bytes, int priority);
}
