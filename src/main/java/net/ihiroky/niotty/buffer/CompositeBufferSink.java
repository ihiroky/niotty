package net.ihiroky.niotty.buffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
/**
 * @author Hiroki Itoh
 */
public class CompositeBufferSink implements BufferSink {

    private BufferSink[] bufferSinks;
    private int sinkIndex;

    CompositeBufferSink(BufferSink ...bufferSinks) {
        this.bufferSinks = bufferSinks;
    }

    @Override
    public boolean transferTo(WritableByteChannel channel, ByteBuffer writeBuffer) throws IOException {
        for (int i = 0; i < sinkIndex; i++) {
            if (!bufferSinks[i].transferTo(channel, writeBuffer)) {
                return false;
            }
        }
        return true;
    }
}
