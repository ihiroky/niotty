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

    private Deque<EncodeBuffer> group = new ArrayDeque<>();
    private BufferSink bufferSink;

    public EncodeBufferGroup addLast(EncodeBuffer encodeBuffer) {
        group.addLast(encodeBuffer);
        return this;
    }

    public EncodeBufferGroup addFirst(EncodeBuffer encodeBuffer) {
        group.addFirst(encodeBuffer);
        return this;
    }

    public EncodeBuffer pollFirst() {
        return group.pollFirst();
    }

    public EncodeBuffer pollLast() {
        return group.pollLast();
    }

    public EncodeBuffer peekFirst() {
        return group.peekFirst();
    }

    public EncodeBuffer peekLast() {
        return group.peekLast();
    }

    @Override
    public Iterator<EncodeBuffer> iterator() {
        return group.iterator();
    }

    public int filledBytes() {
        int sum = 0;
        for (EncodeBuffer encodeBuffer : group) {
            sum += encodeBuffer.filledBytes();
        }
        return sum;
    }

    @Override
    public boolean transferTo(WritableByteChannel channel, ByteBuffer writeBuffer) throws IOException {
        BufferSink bs = bufferSink;
        if (bs == null) {
            bs = bufferSink = Buffers.createBufferSink(group);
        }
        return bs.transferTo(channel, writeBuffer);
    }

    @Override
    public int remainingBytes() {
        return (bufferSink != null) ? bufferSink.remainingBytes() : filledBytes();
    }
}
