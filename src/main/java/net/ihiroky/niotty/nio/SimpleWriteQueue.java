package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.buffer.BufferSink;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author Hiroki Itoh
 */
public class SimpleWriteQueue implements WriteQueue {

    private boolean existsPreviousData_;
    private Queue<BufferSink> queue_;
    private int lastFlushedBytes_;

    public SimpleWriteQueue() {
        queue_ = new ConcurrentLinkedQueue<>();
    }

    @Override
    public boolean offer(BufferSink bufferSink) {
        return queue_.offer(bufferSink);
    }

    @Override
    public FlushStatus flushTo(WritableByteChannel channel, ByteBuffer writeBuffer) throws IOException {
        return flushTo(channel, writeBuffer, Integer.MAX_VALUE);
    }

    FlushStatus flushTo(WritableByteChannel channel, ByteBuffer writeBuffer, int limitBytes) throws IOException {
        int flushedBytes = 0;

        if (existsPreviousData_) {
            limitBytes -= writeBuffer.remaining();
            if (limitBytes < 0) {
                lastFlushedBytes_ = 0;
                return FlushStatus.SKIP;
            }
            int written = channel.write(writeBuffer);
            if (written == -1) {
                lastFlushedBytes_ = 0;
                throw new IOException("end of stream.");
            }
            if (writeBuffer.hasRemaining()) {
                lastFlushedBytes_ = written;
                return FlushStatus.FLUSHING;
            }
            existsPreviousData_ = false;
            flushedBytes += written;
            writeBuffer.clear();
        }

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
            if (pendingBuffer.transferTo(channel, writeBuffer)) {
                flushedBytes += beforeTransfer;
                writeBuffer.clear();
                queue_.poll();
                if (flushedBytes >= limitBytes) {
                    lastFlushedBytes_ = flushedBytes;
                    return queue_.isEmpty() ? FlushStatus.FLUSHED : FlushStatus.FLUSHING;
                }
            } else {
                existsPreviousData_ = true;
                lastFlushedBytes_ = flushedBytes + (beforeTransfer - writeBuffer.remaining());
                if (pendingBuffer.remainingBytes() == 0) {
                    queue_.poll();
                }
                return FlushStatus.FLUSHING;
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
}
