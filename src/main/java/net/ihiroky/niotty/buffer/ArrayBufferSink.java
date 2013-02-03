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

    private byte[][] buffers;

    static final ArrayBufferSink EMPTY_ENCODING_BUFFER = new ArrayBufferSink(new byte[0][0]);

    ArrayBufferSink(byte[] buffer) {
        this.buffers = new byte[][] {buffer};
    }

    ArrayBufferSink(byte[][] buffers) {
        this.buffers = buffers;
    }
    @Override
    public boolean needsDirectTransfer() {
        return false;
    }

    @Override
    public void transferTo(ByteBuffer writeBuffer) {
        for (byte[] buffer : buffers) {
            writeBuffer.put(buffer, 0, buffer.length);
        }
    }

    @Override
    public void transferTo(Queue<ByteBuffer> writeQueue) {
        for (byte[] buffer : buffers) {
            writeQueue.offer(ByteBuffer.wrap(buffer));
        }
    }

    @Override
    public void transferTo(WritableByteChannel channel) {
    }
}
