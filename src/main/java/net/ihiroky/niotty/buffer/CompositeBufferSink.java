package net.ihiroky.niotty.buffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.util.Collection;
import java.util.Objects;

/**
 * @author Hiroki Itoh
 */
public class CompositeBufferSink implements BufferSink {

    private BufferSink[] bufferSinks_;

    private static final BufferSink[] EMPTY = new BufferSink[0];

    CompositeBufferSink(Collection<EncodeBuffer> encodeBuffers) {
        Objects.requireNonNull(encodeBuffers, "encodeBuffers");

        BufferSink[] bs = new BufferSink[encodeBuffers.size()];
        int count = 0;
        for (EncodeBuffer encodeBuffer : encodeBuffers) {
            bs[count++] = encodeBuffer.createBufferSink();
        }
        bufferSinks_ = bs;
    }

    @Override
    public boolean transferTo(WritableByteChannel channel, ByteBuffer writeBuffer) throws IOException {
        for (BufferSink bufferSink : bufferSinks_) {
            if (!bufferSink.transferTo(channel, writeBuffer)) {
                return false;
            }
        }
        bufferSinks_ = EMPTY;
        return true;
    }

    @Override
    public int remainingBytes() {
        long sum = 0;
        for (BufferSink bufferSink : bufferSinks_) {
            sum += bufferSink.remainingBytes();
        }
        return (sum <= Integer.MAX_VALUE) ? (int) sum : Integer.MAX_VALUE;
    }
}
