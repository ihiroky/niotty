package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.buffer.BufferSink;

import java.io.IOException;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Hiroki Itoh
 */
public class DeficitRoundRobinWriteQueue implements NioWriteQueue {

    private final SimpleWriteQueue baseQueue_;
    private final List<SimpleWriteQueue> queueList_;
    private final int[] weights_;
    private final int[] deficitCounter_;
    private final int roundBonus_;
    private int queueIndex_;
    private int lastFlushedBytes_;

    private static final int BASE_WEIGHT = 100;
    private static final int QUEUE_INDEX_BASE = -1;
    private static final float MIN_WEIGHT = 0.05f;
    private static final float MAX_WEIGHT = 1f;

    public DeficitRoundRobinWriteQueue(int writeBufferSize, boolean useDirectBuffer,
                                       int roundBonus, float ...weights) {
        int length = weights.length;
        List<SimpleWriteQueue> ql = new ArrayList<>(length);
        int[] p = new int[length];
        for (int i = 0; i < length; i++) {
            float weight = weights[i];
            if (weight < MIN_WEIGHT || weight > MAX_WEIGHT) {
                throw new IllegalArgumentException("weight must be in [" + MIN_WEIGHT + ", " + MAX_WEIGHT + "]");
            }
            p[i] = Math.round(BASE_WEIGHT * weights[i]);
            ql.add(new SimpleWriteQueue(writeBufferSize, useDirectBuffer));
        }
        baseQueue_ = new SimpleWriteQueue(writeBufferSize, useDirectBuffer);
        queueList_ = ql;
        weights_ = p;
        deficitCounter_ = new int[length];
        roundBonus_ = roundBonus;
        queueIndex_ = QUEUE_INDEX_BASE;
    }

    @Override
    public boolean offer(BufferSink bufferSink) {
        int priority = bufferSink.priority();
        if (priority >= weights_.length) {
            throw new IllegalStateException("unsupported priority:" + priority);
        }
        return (priority < 0) ? baseQueue_.offer(bufferSink) : queueList_.get(priority).offer(bufferSink);
    }

    @Override
    public FlushStatus flushTo(WritableByteChannel channel) throws IOException {
        return flushTo(channel, 0);
    }

    private FlushStatus flushTo(WritableByteChannel channel, int previousFlushedByte) throws IOException {
        int baseQuantum = 0;
        int flushedBytes = previousFlushedByte;
        int queueIndex = queueIndex_;
        if (queueIndex == QUEUE_INDEX_BASE) {
            FlushStatus status = baseQueue_.flushTo(channel);
            baseQuantum = baseQueue_.lastFlushedBytes();
            flushedBytes += baseQuantum;
            if (status == FlushStatus.FLUSHING) {
                countUpDeficitCounters(baseQuantum, 0, queueList_.size());
                lastFlushedBytes_ = flushedBytes;
                return status;
            }
            if (baseQuantum == 0) {
                baseQuantum = roundBonus_;
            }
            queueIndex = 0;
        }

        int size = queueList_.size();
        boolean existsSkipped = false;
        for (int i = queueIndex; i < size; i++) {
            SimpleWriteQueue q = queueList_.get(i);

            if (q.isEmpty()) {
                deficitCounter_[i] = 0;
                continue;
            }

            // flush operation
            int newDeficitCounter = deficitCounter_[i] + baseQuantum * weights_[i] / BASE_WEIGHT;
            FlushStatus status = q.flushTo(channel, newDeficitCounter);
            int qlf = q.lastFlushedBytes();
            flushedBytes += qlf;
            deficitCounter_[i] = newDeficitCounter - qlf;
            if (status == FlushStatus.FLUSHING) {
                if (baseQuantum > 0) {
                    // Count up if deficit counter has not been counted up yet.
                    countUpDeficitCounters(baseQuantum, i + 1, size);
                }
                queueIndex_ = i;
                lastFlushedBytes_ = flushedBytes;
                return status;
            } else if (status == FlushStatus.SKIP) {
                existsSkipped = true;
            }
        }
        lastFlushedBytes_ = flushedBytes;

        // finish if baseQueue_ is already checked.
        if (queueIndex_ == QUEUE_INDEX_BASE) {
            return existsSkipped ? FlushStatus.SKIP : FlushStatus.FLUSHED;
        }

        // one more round if this method starts in queueList.
        queueIndex_ = QUEUE_INDEX_BASE;
        return baseQueue_.isEmpty()
                ? (existsSkipped ? FlushStatus.SKIP : FlushStatus.FLUSHED)
                : flushTo(channel, flushedBytes);
    }

    private void countUpDeficitCounters(int baseQuantum, int from, int size) {
        SimpleWriteQueue q;
        for (int i = from; i < size; i++) {
            q = queueList_.get(i);
            deficitCounter_[i] = q.isEmpty() ? 0 : baseQuantum * weights_[i] / BASE_WEIGHT;
        }
    }

    @Override
    public int size() {
        long sum = baseQueue_.size();
        for (SimpleWriteQueue queue : queueList_) {
            sum += queue.size();
        }
        return (sum <= Integer.MAX_VALUE) ? (int) sum : Integer.MAX_VALUE;
    }

    @Override
    public boolean isEmpty() {
        if (!baseQueue_.isEmpty()) {
            return false;
        }
        for (SimpleWriteQueue queue : queueList_) {
            if (!queue.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int lastFlushedBytes() {
        return lastFlushedBytes_;
    }

    int roundBonus() {
        return roundBonus_;
    }

    int weights(int priority) {
        if (priority < 0 || priority >= weights_.length) {
            throw new IndexOutOfBoundsException();
        }
        return weights_[priority];
    }

    int deficitCounter(int priority) {
        if (priority < 0 || priority >= deficitCounter_.length) {
            throw new IndexOutOfBoundsException();
        }
        return deficitCounter_[priority];
    }

    void deficitCounter(int priority, int counter) {
        if (priority < 0 || priority >= deficitCounter_.length) {
            throw new IndexOutOfBoundsException();
        }
        deficitCounter_[priority] = counter;
    }

    int queueIndex() {
        return queueIndex_;
    }

    void queueIndex(int i) {
        if (i <= 0 && i >= queueList_.size()) {
            throw new IndexOutOfBoundsException();
        }
        queueIndex_ = i;
    }
}
