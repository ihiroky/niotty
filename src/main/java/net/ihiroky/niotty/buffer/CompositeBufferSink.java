package net.ihiroky.niotty.buffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.util.Objects;

/**
 * @author Hiroki Itoh
 */
public class CompositeBufferSink implements BufferSink {

    private BufferSink car;
    private BufferSink cdr;

    CompositeBufferSink(BufferSink car, BufferSink cdr) {
        Objects.requireNonNull(car, "car");
        Objects.requireNonNull(cdr, "cdr");
        this.car = car;
        this.cdr = cdr;
    }

    @Override
    public boolean transferTo(WritableByteChannel channel, ByteBuffer writeBuffer) throws IOException {
        return car.transferTo(channel, writeBuffer) && cdr.transferTo(channel, writeBuffer);
    }

    @Override
    public int remainingBytes() {
        long sum = car.remainingBytes() + cdr.remainingBytes();
        return (sum <= Integer.MAX_VALUE) ? (int) sum : Integer.MAX_VALUE;
    }
}
