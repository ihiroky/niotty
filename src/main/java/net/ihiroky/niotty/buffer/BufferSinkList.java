package net.ihiroky.niotty.buffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.util.Objects;

/**
 * Holds a pair of {@link net.ihiroky.niotty.buffer.BufferSink}.
 * This class is also {@code BufferSink}.
 *
 * @author Hiroki Itoh
 */
public class BufferSinkList implements BufferSink {

    private BufferSink car_;
    private BufferSink cdr_;
    private int priority_;

    BufferSinkList(BufferSink car, BufferSink cdr) {
        this(car, cdr, Buffers.DEFAULT_PRIORITY);
    }

    BufferSinkList(BufferSink car, BufferSink cdr, int priority) {
        Objects.requireNonNull(car, "car");
        Objects.requireNonNull(cdr, "cdr");
        car_ = car;
        cdr_ = cdr;
        priority_ = priority;
    }

    /**
     * Writes data in the pair which is given order at the constructor.
     * {@inheritDoc}
     */
    @Override
    public boolean transferTo(WritableByteChannel channel, ByteBuffer writeBuffer) throws IOException {
        return car_.transferTo(channel, writeBuffer) && cdr_.transferTo(channel, writeBuffer);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int remainingBytes() {
        long sum = car_.remainingBytes() + cdr_.remainingBytes();
        return (sum <= Integer.MAX_VALUE) ? (int) sum : Integer.MAX_VALUE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int priority() {
        return priority_;
    }
}
