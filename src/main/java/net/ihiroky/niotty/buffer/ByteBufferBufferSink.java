package net.ihiroky.niotty.buffer;

import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.util.Queue;

/**
 * @author Hiroki Itoh
 */
public class ByteBufferBufferSink implements BufferSink {

    private ByteBuffer byteBuffer;

    ByteBufferBufferSink(ByteBuffer byteBuffer) {
        this.byteBuffer= byteBuffer;
    }

    @Override
    public boolean needsDirectTransfer() {
        return false;
    }

    @Override
    public void transferTo(ByteBuffer writeBuffer) {
        writeBuffer.put(byteBuffer);
    }

    @Override
    public void transferTo(Queue<ByteBuffer> writeQueue) {
        writeQueue.offer(byteBuffer);
    }

    @Override
    public void transferTo(WritableByteChannel channel) {
    }
}
