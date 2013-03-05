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
public class CodecBufferDeque implements Iterable<CodecBuffer>, BufferSink {

    private Deque<CodecBuffer> deque_ = new ArrayDeque<>(INITIAL_GROUP_CAPACITY);

    private static final int INITIAL_GROUP_CAPACITY = 4; // actually 8 in ArrayDeque.

    public CodecBufferDeque addLast(CodecBuffer encodeBuffer) {
        deque_.addLast(encodeBuffer);
        return this;
    }

    public CodecBufferDeque addFirst(CodecBuffer encodeBuffer) {
        deque_.addFirst(encodeBuffer);
        return this;
    }

    public CodecBuffer pollFirst() {
        return deque_.pollFirst();
    }

    public CodecBuffer pollLast() {
        return deque_.pollLast();
    }

    public CodecBuffer peekFirst() {
        return deque_.peekFirst();
    }

    public CodecBuffer peekLast() {
        return deque_.peekLast();
    }

    @Override
    public Iterator<CodecBuffer> iterator() {
        return deque_.iterator();
    }

    @Override
    public boolean transferTo(WritableByteChannel channel, ByteBuffer writeBuffer) throws IOException {
        for (BufferSink bufferSink : deque_) {
            if (!bufferSink.transferTo(channel, writeBuffer)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int remainingBytes() {
        long sum = 0;
        for (BufferSink bufferSink : deque_) {
            sum += bufferSink.remainingBytes();
        }
        return (sum <= Integer.MAX_VALUE) ? (int) sum : Integer.MAX_VALUE;
    }

    @Override
    public int priority() {
        return 0;
    }
}
