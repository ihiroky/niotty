package net.ihiroky.niotty.buffer;

import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.util.Queue;

/**
 * Created on 13/02/01, 17:38
 *
 * @author Hiroki Itoh
 */
class ArrayBufferSink implements BufferSink {

    private byte[] buffer;

    static final ArrayBufferSink EMPTY_ENCODING_BUFFER =
            new ArrayBufferSink(new byte[0]);

    ArrayBufferSink(byte[] buffer) {
        this.buffer = buffer;
    }
    @Override
    public boolean needsDirectTransfer() {
        return false;
    }

    @Override
    public void transferTo(ByteBuffer writeBuffer) {
        writeBuffer.put(buffer, 0, buffer.length);
    }

    @Override
    public void transferTo(Queue<ByteBuffer> writeQueue) {
        writeQueue.offer(ByteBuffer.wrap(buffer));
    }

    @Override
    public void transferTo(WritableByteChannel channel) {
    }
}
