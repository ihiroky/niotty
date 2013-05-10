package net.ihiroky.niotty.buffer;

import java.nio.ByteBuffer;

/**
 * @author Hiroki Itoh
 */
public class PriorityByteBufferCodecBuffer extends ByteBufferCodecBuffer {

    private final int priority_;

    PriorityByteBufferCodecBuffer(int priority) {
        super();
        priority_ = priority;
    }

    PriorityByteBufferCodecBuffer(int initialCapacity, int priority) {
        super(ByteBufferChunkFactory.heap(), initialCapacity);
        priority_ = priority;
    }

    PriorityByteBufferCodecBuffer(ByteBuffer byteBuffer, int priority) {
        super(byteBuffer);
        priority_ = priority;
    }

    @Override
    public int priority() {
        return priority_;
    }
}
