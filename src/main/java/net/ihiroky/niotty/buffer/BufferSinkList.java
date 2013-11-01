package net.ihiroky.niotty.buffer;

import net.ihiroky.niotty.util.Arguments;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.GatheringByteChannel;

/**
 * Holds a pair of {@link BufferSink}.
 * This class itself is also {@code BufferSink}.
 *
 * @author Hiroki Itoh
 */
public class BufferSinkList implements BufferSink {

    private final BufferSink car_;
    private final BufferSink cdr_;

    BufferSinkList(BufferSink car, BufferSink cdr) {
        Arguments.requireNonNull(car, "car");
        Arguments.requireNonNull(cdr, "cdr");
        car_ = car;
        cdr_ = cdr;
    }

    @Override
    public boolean transferTo(GatheringByteChannel channel) throws IOException {
        return car_.transferTo(channel) && cdr_.transferTo(channel);
    }

    @Override
    public void copyTo(ByteBuffer buffer) {
        car_.copyTo(buffer);
        cdr_.copyTo(buffer);
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
        int carRemaining = car_.remaining();
        int cdrRemaining = cdr_.remaining();
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
            return (carSliced != null) ? Buffers.wrap(carSliced, cdr_.slice(bytes)) : cdr_.slice(bytes);
        }

        // empty && bytes == 0
        return Buffers.newCodecBuffer(0);
    }

    @Override
    public BufferSinkList duplicate() {
        return new BufferSinkList(car_.duplicate(), cdr_.duplicate());
    }

    @Override
    public boolean hasArray() {
        return false;
    }

    @Override
    public byte[] array() {
        int remaining = remaining();
        ByteBuffer bb = ByteBuffer.allocate(remaining);
        copyTo(bb);
        return bb.array();
    }

    @Override
    public int arrayOffset() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int remaining() {
        long sum = car_.remaining() + cdr_.remaining();
        return (sum <= Integer.MAX_VALUE) ? (int) sum : Integer.MAX_VALUE;
    }

    @Override
    public void dispose() {
        car_.dispose();
        cdr_.dispose();
    }

    @Override
    public String toString() {
        return "[" + car_ + ", " + cdr_ + "]";
    }

    BufferSink car() {
        return car_;
    }

    BufferSink cdr() {
        return cdr_;
    }
}
