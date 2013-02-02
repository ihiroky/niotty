package net.ihiroky.niotty.buffer;

import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.util.Queue;
/**
 * @author Hiroki Itoh
 */
public class CompositeBufferSink implements BufferSink {

    private BufferSink[] bufferSinks;

    CompositeBufferSink(BufferSink ...bufferSinks) {
        this.bufferSinks = bufferSinks;
    }

    @Override
    public boolean needsDirectTransfer() {
        // TODO bufferSinks may contains true and false
        return false;
    }

    @Override
    public void transferTo(ByteBuffer writeBuffer) {
        for (BufferSink bufferSink : bufferSinks) {
            bufferSink.transferTo(writeBuffer);
        }
    }

    @Override
    public void transferTo(Queue<ByteBuffer> writeQueue) {
        for (BufferSink bufferSink : bufferSinks) {
            bufferSink.transferTo(writeQueue);
        }
    }

    @Override
    public void transferTo(WritableByteChannel channel) {
        for (BufferSink bufferSink : bufferSinks) {
            bufferSink.transferTo(channel);
        }
    }
}
