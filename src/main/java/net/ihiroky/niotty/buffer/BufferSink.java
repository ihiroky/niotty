package net.ihiroky.niotty.buffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;

/**
 * Add interface if new Transport is added and new data type is required.
 * @author Hiroki Itoh
 */
public interface BufferSink {
    boolean transferTo(WritableByteChannel channel, ByteBuffer writeBuffer) throws IOException;
}
