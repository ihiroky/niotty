package net.ihiroky.niotty.codec;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.GatheringByteChannel;

/**
 * @author Hiroki Itoh
 */
public class BufferChannel implements GatheringByteChannel {

    private byte[] buffer_;
    private int position_;

    BufferChannel(byte[] buffer) {
        buffer_ = buffer;
        position_ = 0;
    }

    byte[] array() {
        return buffer_;
    }

    int position() {
        return position_;
    }

    void reset() {
        position_ = 0;
    }

    @Override
    public long write(ByteBuffer[] srcs, int offset, int length) throws IOException {
        byte[] buffer = buffer_;
        int position = position_;
        int end = offset + length;
        for (int i = offset; i < end; i++) {
            int remaining = srcs[i].remaining();
            int space = buffer.length - position;
            if (remaining <= space) {
                srcs[i].get(buffer, position, remaining);
                position += remaining;
            } else {
                srcs[i].get(buffer, position, space);
                position += space;
                break;
            }
        }
        int written = position - position_;
        position_ = position;
        return written;
    }

    @Override
    public long write(ByteBuffer[] srcs) throws IOException {
        byte[] buffer = buffer_;
        int position = position_;
        for (ByteBuffer src : srcs) {
            int remaining = src.remaining();
            int space = buffer.length - position;
            if (remaining <= space) {
                src.get(buffer, position, remaining);
                position += remaining;
            } else {
                src.get(buffer, position, space);
                position += space;
                break;
            }
        }
        int written = position - position_;
        position_ = position;
        return written;
    }

    @Override
    public int write(ByteBuffer src) throws IOException {
        int remaining = src.remaining();
        int space = buffer_.length - position_;
        if (space >= remaining) {
            src.get(buffer_, position_, remaining);
            position_ += remaining;
            return remaining;
        }
        src.get(buffer_, position_, space);
        position_ = buffer_.length;
        return space;
    }

    @Override
    public boolean isOpen() {
        return true;
    }

    @Override
    public void close() throws IOException {
    }
}
