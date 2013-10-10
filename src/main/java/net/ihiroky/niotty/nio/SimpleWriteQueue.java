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

    private Queue<AttachedMessage<BufferSink>> queue_;
    private int lastFlushedBytes_;

    SimpleWriteQueue() {
        queue_ = new ConcurrentLinkedQueue<AttachedMessage<BufferSink>>();
    }

    @Override
    public boolean offer(AttachedMessage<BufferSink> message) {
        return queue_.offer(message);
    }

    @Override
    public FlushStatus flushTo(GatheringByteChannel channel) throws IOException {
        return flushTo(channel, Integer.MAX_VALUE);
    }

    FlushStatus flushTo(GatheringByteChannel channel, int limitBytes) throws IOException {
        int flushedBytes = 0;

        for (;;) {
            AttachedMessage<BufferSink> message = queue_.peek();
            if (message == null) {
                lastFlushedBytes_ = flushedBytes;
                return FlushStatus.FLUSHED;
            }

            BufferSink buffer = message.message();
            int beforeTransfer = buffer.remainingBytes();
            limitBytes -= beforeTransfer;
            if (limitBytes < 0) {
                lastFlushedBytes_ = flushedBytes;
                return FlushStatus.SKIPPED;
            }
            if (buffer.transferTo(channel)) {
                flushedBytes += beforeTransfer;
                buffer.dispose();
                queue_.poll();
                if (flushedBytes >= limitBytes) {
                    lastFlushedBytes_ = flushedBytes;
                    return queue_.isEmpty() ? FlushStatus.FLUSHED : FlushStatus.SKIPPED;
                }
            } else {
                lastFlushedBytes_ = flushedBytes + (beforeTransfer - buffer.remainingBytes());
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

        for (;;) {
            AttachedMessage<BufferSink> message = queue_.peek();
            if (message == null) {
                lastFlushedBytes_ = flushedBytes;
                return FlushStatus.FLUSHED;
            }

            BufferSink buffer = message.message();
            byteBuffer.clear();
            buffer.copyTo(byteBuffer);
            byteBuffer.flip();
            SocketAddress target = (SocketAddress) message.parameter().argument();

            // transfer all data or not.
            int transferred = (target != null) ? channel.send(byteBuffer, target) : channel.write(byteBuffer);

            if (transferred > 0) {
                flushedBytes += transferred;
                buffer.dispose();
                queue_.poll();
                if (flushedBytes >= limitBytes) {
                    lastFlushedBytes_ = flushedBytes;
                    return queue_.isEmpty() ? FlushStatus.FLUSHED : FlushStatus.SKIPPED;
                }
            } else {
                lastFlushedBytes_ = flushedBytes;
                return FlushStatus.SKIPPED;
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
        for (AttachedMessage<BufferSink> message : queue_) {
            message.message().dispose();
        }
        queue_.clear();
    }
}
