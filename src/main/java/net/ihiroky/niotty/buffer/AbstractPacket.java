package net.ihiroky.niotty.buffer;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

/**
 *
 */
public abstract class AbstractPacket implements Packet {

    @Override
    public boolean sink(DatagramChannel channel, ByteBuffer buffer, SocketAddress target) throws IOException {
        copyTo(buffer);
        buffer.flip();
        int remaining = buffer.remaining();
        boolean sent = (channel.send(buffer, target) == remaining);
        buffer.clear();
        return sent;
    }

    @Override
    public ByteBuffer byteBuffer() {
        int remaining = remaining();
        ByteBuffer bb = ByteBuffer.allocate(remaining);
        copyTo(bb);
        bb.flip();
        return bb;
    }

    /**
     * <p>Tells whether or not this buffer is equal to another object.</p>
     * <p>If the object is an instance of {@code Packet},
     * then this method uses a temporary temporary byte buffer created by
     * {@link #byteBuffer()} to compare these contents.</p>
     *
     * @param object the object
     * @return  true if, and only if, this buffer is equal to the given object
     */
    @Override
    public boolean equals(Object object) {
        if (object == this) {
            return true;
        }
        if (object instanceof Packet) {
            Packet that = (Packet) object;
            return this.byteBuffer().equals(that.byteBuffer());
        }
        return false;
    }

    /**
     * <p>Returns the current hash code of this buffer.</p>
     * <p>then this method uses a temporary temporary byte buffer created by
     * {@link #byteBuffer()} to calculate the hash code</p>
     * @return the current hash code of this buffer
     */
    @Override
    public int hashCode() {
        return byteBuffer().hashCode();
    }
}
