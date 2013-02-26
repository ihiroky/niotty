package net.ihiroky.niotty.buffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

/**
 * @author Hiroki Itoh
 */
public class EncodeBufferGroup implements Iterable<EncodeBuffer>, BufferSink {

    private Deque<EncodeBuffer> group_ = new ArrayDeque<>();
    private BufferSink bufferSink_;

    public EncodeBufferGroup addLast(EncodeBuffer encodeBuffer) {
        group_.addLast(encodeBuffer);
        return this;
    }

    public EncodeBufferGroup addFirst(EncodeBuffer encodeBuffer) {
        group_.addFirst(encodeBuffer);
        return this;
    }

    public EncodeBuffer pollFirst() {
        return group_.pollFirst();
    }

    public EncodeBuffer pollLast() {
        return group_.pollLast();
    }

    public EncodeBuffer peekFirst() {
        return group_.peekFirst();
    }

    public EncodeBuffer peekLast() {
        return group_.peekLast();
    }

    @Override
    public Iterator<EncodeBuffer> iterator() {
        return group_.iterator();
    }

    public int filledBytes() {
        int sum = 0;
        for (EncodeBuffer encodeBuffer : group_) {
            sum += encodeBuffer.filledBytes();
        }
        return sum;
    }

    @Override
    public boolean transferTo(WritableByteChannel channel, ByteBuffer writeBuffer) throws IOException {
        BufferSink bs = bufferSink_;
        if (bs == null) {
            bs = bufferSink_ = Buffers.createBufferSink(group_);
        }
        return bs.transferTo(channel, writeBuffer);
    }

    @Override
    public int remainingBytes() {
        return (bufferSink_ != null) ? bufferSink_.remainingBytes() : filledBytes();
    }
}
