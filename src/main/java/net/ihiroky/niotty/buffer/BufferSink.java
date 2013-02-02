package net.ihiroky.niotty.buffer;

import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.util.Queue;

/**
 * @author Hiroki Itoh
 */
public interface BufferSink {
    boolean needsDirectTransfer();
    void transferTo(ByteBuffer writeBuffer);
    void transferTo(Queue<ByteBuffer> writeQueue);
    void transferTo(WritableByteChannel channel);
}
