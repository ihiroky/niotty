package net.ihiroky.niotty.buffer;

import java.io.IOException;
import java.nio.channels.GatheringByteChannel;
import java.util.Objects;

/**
 * Holds a pair of {@link net.ihiroky.niotty.buffer.BufferSink}.
 * This class itself is also {@code BufferSink}.
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

    @Override
    public boolean transferTo(GatheringByteChannel channel) throws IOException {
        return car_.transferTo(channel) && cdr_.transferTo(channel);
    }

    @Override
    public BufferSinkList addFirst(CodecBuffer buffer) {
        car_.addFirst(buffer);
        return this;
    }

    @Override
    public BufferSinkList addLast(CodecBuffer buffer) {
        cdr_.addLast(buffer);
        return this;
    }

    @Override
    public BufferSink slice(int bytes) {
        int carRemaining = car_.remainingBytes();
        int cdrRemaining = cdr_.remainingBytes();
        if (bytes < 0 || bytes > carRemaining + cdrRemaining) {
            throw new IllegalArgumentException("Invalid input " + bytes + ". "
                    + (carRemaining + cdrRemaining) + " byte remains.");
        }
        BufferSink carSliced = null;
        if (carRemaining > 0) {
            if (carRemaining >= bytes) {
                return car_.slice(bytes);
            }
            carSliced = car_.slice(carRemaining);
            bytes -= carRemaining;
        }

        if (cdrRemaining > 0) {
            return (carSliced != null) ? Buffers.newBufferSink(carSliced, cdr_.slice(bytes)) : cdr_.slice(bytes);
        }

        // empty && bytes == 0
        return Buffers.newCodecBuffer(0, priority_);
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

    @Override
    public void dispose() {
        car_.dispose();
        cdr_.dispose();
    }

    BufferSink car() {
        return car_;
    }

    BufferSink cdr() {
        return cdr_;
    }
}
