package net.ihiroky.niotty.buffer;

import net.ihiroky.niotty.util.Arguments;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.GatheringByteChannel;

/**
 * Holds a pair of {@link Packet}.
 * This class itself is also {@code Packet}.
 *
 * @author Hiroki Itoh
 */
public class PacketList extends AbstractPacket {

    private final Packet car_;
    private final Packet cdr_;

    PacketList(Packet car, Packet cdr) {
        Arguments.requireNonNull(car, "car");
        Arguments.requireNonNull(cdr, "cdr");
        car_ = car;
        cdr_ = cdr;
    }

    @Override
    public boolean sink(GatheringByteChannel channel) throws IOException {
        return car_.sink(channel) && cdr_.sink(channel);
    }

    @Override
    public void copyTo(ByteBuffer buffer) {
        car_.copyTo(buffer);
        cdr_.copyTo(buffer);
    }


    @Override
    public PacketList addFirst(CodecBuffer buffer) {
        car_.addFirst(buffer);
        return this;
    }

    @Override
    public PacketList addLast(CodecBuffer buffer) {
        cdr_.addLast(buffer);
        return this;
    }

    @Override
    public Packet slice(int bytes) {
        int carRemaining = car_.remaining();
        int cdrRemaining = cdr_.remaining();
        if (bytes < 0 || bytes > carRemaining + cdrRemaining) {
            throw new IllegalArgumentException("Invalid input " + bytes + ". "
                    + (carRemaining + cdrRemaining) + " byte remains.");
        }
        Packet carSliced = null;
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
    public PacketList duplicate() {
        return new PacketList(car_.duplicate(), cdr_.duplicate());
    }

    @Override
    public boolean hasArray() {
        return false;
    }

    @Override
    public byte[] array() {
        return byteBuffer().array();
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

    Packet car() {
        return car_;
    }

    Packet cdr() {
        return cdr_;
    }
}
