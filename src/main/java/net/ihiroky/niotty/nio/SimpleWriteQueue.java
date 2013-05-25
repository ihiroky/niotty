package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.buffer.BufferSink;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.GatheringByteChannel;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Implementation of {@link net.ihiroky.niotty.nio.WriteQueue} using single queue.
 *
 * @author Hiroki Itoh
 */
public class SimpleWriteQueue implements WriteQueue {

    private Queue<BufferSink> queue_;
    private int lastFlushedBytes_;

    SimpleWriteQueue() {
        queue_ = new ConcurrentLinkedQueue<>();
    }

    @Override
    public boolean offer(BufferSink bufferSink) {
        return queue_.offer(bufferSink);
    }

    @Override
    public FlushStatus flushTo(GatheringByteChannel channel) throws IOException {
        return flushTo(channel, Integer.MAX_VALUE);
    }

    FlushStatus flushTo(GatheringByteChannel channel, int limitBytes) throws IOException {
        int flushedBytes = 0;

        for (;;) {
            BufferSink pendingBuffer = queue_.peek();
            if (pendingBuffer == null) {
                lastFlushedBytes_ = flushedBytes;
                return FlushStatus.FLUSHED;
            }
            int beforeTransfer = pendingBuffer.remainingBytes();
            limitBytes -= beforeTransfer;
            if (limitBytes < 0) {
                lastFlushedBytes_ = flushedBytes;
                return FlushStatus.SKIP;
            }
            if (pendingBuffer.transferTo(channel)) {
                flushedBytes += beforeTransfer;
                queue_.poll();
                if (flushedBytes >= limitBytes) {
                    lastFlushedBytes_ = flushedBytes;
                    return queue_.isEmpty() ? FlushStatus.FLUSHED : FlushStatus.SKIP;
                }
            } else {
                lastFlushedBytes_ = flushedBytes + (beforeTransfer - pendingBuffer.remainingBytes());
                return FlushStatus.FLUSHING;
            }
        }
    }

    // TODO change the writeBuffer to a chunk pool to avoid BufferOverflowException.
    @Override
    public FlushStatus flushTo(DatagramChannel channel, ByteBuffer writeBuffer) throws IOException {
        return flushTo(channel, writeBuffer, Integer.MAX_VALUE);
    }

    FlushStatus flushTo(DatagramChannel channel, ByteBuffer byteBuffer, int limitBytes) throws IOException {
        int flushedBytes = 0;

        byteBuffer.clear();
        for (;;) {
            BufferSink pendingBuffer = queue_.peek();
            if (pendingBuffer == null) {
                lastFlushedBytes_ = flushedBytes;
                return FlushStatus.FLUSHED;
            }

            pendingBuffer.transferTo(byteBuffer);
            byteBuffer.flip();
            SocketAddress target = pendingBuffer.attachment().socketAddress();

            // transfer all data or not.
            int transferred = (target != null) ? channel.send(byteBuffer, target) : channel.write(byteBuffer);

            if (transferred > 0) {
                flushedBytes += transferred;
                queue_.poll();
                if (flushedBytes >= limitBytes) {
                    lastFlushedBytes_ = flushedBytes;
                    return queue_.isEmpty() ? FlushStatus.FLUSHED : FlushStatus.SKIP;
                }
            } else {
                lastFlushedBytes_ = flushedBytes;
                return FlushStatus.SKIP;
            }
        }
    }

    @Override
    public int size() {
        return queue_.size();
    }

    @Override
    public boolean isEmpty() {
        return queue_.isEmpty();
    }

    @Override
    public int lastFlushedBytes() {
        return lastFlushedBytes_;
    }

    @Override
    public void clear() {
        for (BufferSink bufferSink : queue_) {
            bufferSink.dispose();
        }
        queue_.clear();
    }
}
