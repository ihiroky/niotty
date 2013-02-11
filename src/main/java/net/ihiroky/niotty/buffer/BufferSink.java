package net.ihiroky.niotty.buffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;

/**
 * Writes data into a given {@code java.nio.channel.WritableByteChannel}. {@code java.nio.ByteBuffer} is also given,
 * which is allocated per the channel and fixed size to support write operation.
 *
 * Add interface if new Transport is added and new data type is required.
 * @author Hiroki Itoh
 */
public interface BufferSink {
    /**
     * Write data in this instance to the given {@code channel} using the given {@code writeBuffer}.
     *
     * @param channel the {@code WritableByteChannel} to be written into
     * @param writeBuffer the buffer to write data into the {@code channel}
     * @return true if all data in this instance is written into the {@code channel}
     * @throws IOException
     */
    boolean transferTo(WritableByteChannel channel, ByteBuffer writeBuffer) throws IOException;

    /**
     * The byte size of remaining data in this instance.
     * @return the byte size of remaining data in this instance.
     */
    int remainingBytes();
}
