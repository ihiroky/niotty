package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.TransportParameter;
import net.ihiroky.niotty.buffer.BufferSink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.GatheringByteChannel;
import java.util.ArrayList;
import java.util.List;

/**
 * A implementation of {@link net.ihiroky.niotty.nio.WriteQueue} using
 * <a ref="http://en.wikipedia.org/wiki/Deficit_round_robin">deficit round robin</a> algorithm.
 *
 * This class has a base queue and weighted queues. The all queues is {@link net.ihiroky.niotty.nio.SimpleWriteQueue}.
 * These are checked and flushed in a round which is executed by calling
 * {@link #flushTo(java.nio.channels.GatheringByteChannel, java.nio.ByteBuffer)}.
 * The base queue is a basis of flush size of calculation. The flush size for the base queue is limited by
 * {@code baseQueueLimit} specified by its constructor. If some packets ({@link net.ihiroky.niotty.buffer.BufferSink})
 * are in the base queue, a flush operation for the base queue is executed. The basis of flush size
 * is a result of the operation. On the other hand, the flush size of the weighted queues are limited
 * by the actual flush size of the base queue and their weight, which is a rate against the base queue.
 * The weight is in [5%, 100%]. The flush sizes of the weighted queues, as known as deficit counter, is accumulated
 * across the rounds. If the counters get over the size of an element in the weighted queues, then the element is
 * flushed and the counter is decremented by its actual flush size.
 *
 * A priority of the {@link net.ihiroky.niotty.buffer.BufferSink} passed at
 * {@link #offer(net.ihiroky.niotty.buffer.BufferSink)} decides the queue to be inserted.
 * If the priority is negative, then the {@code BufferSink} is inserted to the base queue. If positive, then it
 * is inserted to the weighted queue, the same order as {@code weights} specified by the constructor.
 *
 * @author Hiroki Itoh
 */
public class DeficitRoundRobinWriteQueue implements WriteQueue {

    private final SimpleWriteQueue baseQueue_;
    private final List<SimpleWriteQueue> weightedQueueList_;
    private final int[] weights_;
    private final int[] deficitCounter_;
    private final int baseQueueLimit_;
    private int queueIndex_;
    private int lastFlushedBytes_;
    private Logger logger_ = LoggerFactory.getLogger(DeficitRoundRobinWriteQueue.class);

    private static final int BASE_WEIGHT = 100;
    private static final int QUEUE_INDEX_BASE = -1;
    private static final float MIN_WEIGHT = 0.05f;
    private static final float MAX_WEIGHT = 1f;
    private static final int BONUS_QUANTUM_DIVISOR = 2;

    /**
     * Creates a instance of {@code DeficitRoundRobinWriteQueue}.
     * The weighed queues are created according to a specified {@code weights}.
     * The {@code weights} should be a instance created by {@link #convertWeightsRateToPercent(int, float...)}.
     *
     * @param baseQueueLimit a flush size limitation of the base queue
     * @param weights array of the weight for the weighted queues, the weight unit is percent
     */
    DeficitRoundRobinWriteQueue(int baseQueueLimit, int ...weights) {
        int length = weights.length;
        List<SimpleWriteQueue> ql = new ArrayList<>(length);
        for (int i = 0; i < length; i++) {
            ql.add(new SimpleWriteQueue());
        }
        baseQueue_ = new SimpleWriteQueue();
        weightedQueueList_ = ql;
        weights_ = weights.clone();
        deficitCounter_ = new int[length];
        baseQueueLimit_ = baseQueueLimit;
        queueIndex_ = QUEUE_INDEX_BASE;
    }

    /**
     * Converts specified {@code weights} unit from rate to percent.
     *
     * @param baseQueueLimit a base queue limit
     * @param weights array of the weight which is rate against the base queue, each elements must be in [0.05, 1].
     * @return array of the weight, which is percent
     * @throws IllegalArgumentException if the element of {@code weights} is not in [0.05, 1],
     *    or it is possible that the result of the flush size calculation overflows
     */
    static int[] convertWeightsRateToPercent(int baseQueueLimit, float... weights) {
        int length = weights.length;
        int[] ps = new int[length];
        if (baseQueueLimit < BASE_WEIGHT) {
            throw new IllegalArgumentException("baseQueueLimit must be >= " + BASE_WEIGHT);
        }
        for (int i = 0; i < length; i++) {
            float weight = weights[i];
            if (weight < MIN_WEIGHT || weight > MAX_WEIGHT) {
                throw new IllegalArgumentException("weight must be in [" + MIN_WEIGHT + ", " + MAX_WEIGHT + "]");
            }
            int p = Math.round(BASE_WEIGHT * weights[i]);
            if (p > Integer.MAX_VALUE / baseQueueLimit) {
                throw new IllegalArgumentException("quantum for queue " + i + " might overflow.");
            }
            ps[i] = p;
        }
        return ps;
    }

    @Override
    public boolean offer(BufferSink bufferSink) {
        TransportParameter p = bufferSink.attachment();
        if (p.priority() < 0) {
            return baseQueue_.offer(bufferSink);
        }

        int priority = p.priority();
        if (priority >= weights_.length) {
            throw new IllegalStateException("unsupported priority:" + p.priority());
        }
        return weightedQueueList_.get(priority).offer(bufferSink);
    }

    @Override
    public FlushStatus flushTo(GatheringByteChannel channel, ByteBuffer writeBuffer) throws IOException {
        return flushTo(channel, writeBuffer, 0);
    }

    private FlushStatus flushTo(
            GatheringByteChannel channel, ByteBuffer writeBuffer, int previousFlushedByte) throws IOException {
        int baseQuantum = 0;
        int flushedBytes = previousFlushedByte;
        int queueIndex = queueIndex_;
        if (queueIndex == QUEUE_INDEX_BASE) {
            FlushStatus status = baseQueue_.flushTo(channel, writeBuffer, baseQueueLimit_);
            baseQuantum = baseQueue_.lastFlushedBytes();
            flushedBytes += baseQuantum;
            if (status == FlushStatus.FLUSHING) {
                countUpDeficitCounters(baseQuantum, 0, weightedQueueList_.size());
                lastFlushedBytes_ = flushedBytes;
                return status;
            }
            queueIndex = 0;
        }
        if (baseQuantum == 0) {
            baseQuantum = baseQueueLimit_ / BONUS_QUANTUM_DIVISOR;
        }

        int size = weightedQueueList_.size();
        boolean existsSkipped = false;
        for (int i = queueIndex; i < size; i++) {
            SimpleWriteQueue q = weightedQueueList_.get(i);

            if (q.isEmpty()) {
                deficitCounter_[i] = 0;
                continue;
            }

            // flush operation
            int newDeficitCounter = deficitCounter_[i] + baseQuantum * weights_[i] / BASE_WEIGHT;
            FlushStatus status = q.flushTo(channel, writeBuffer, newDeficitCounter);
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
                : flushTo(channel, writeBuffer, flushedBytes);
    }

    private void countUpDeficitCounters(int baseQuantum, int from, int size) {
        SimpleWriteQueue q;
        for (int i = from; i < size; i++) {
            q = weightedQueueList_.get(i);
            deficitCounter_[i] = q.isEmpty() ? 0 : baseQuantum * weights_[i] / BASE_WEIGHT;
        }
    }

    @Override
    public int size() {
        long sum = baseQueue_.size();
        for (SimpleWriteQueue queue : weightedQueueList_) {
            sum += queue.size();
        }
        return (sum <= Integer.MAX_VALUE) ? (int) sum : Integer.MAX_VALUE;
    }

    @Override
    public boolean isEmpty() {
        if (!baseQueue_.isEmpty()) {
            return false;
        }
        for (SimpleWriteQueue queue : weightedQueueList_) {
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

    @Override
    public void clear() {
        baseQueue_.clear();
        for (SimpleWriteQueue queue : weightedQueueList_) {
            queue.clear();
        }
    }

    int baseQueueLimit() {
        return baseQueueLimit_;
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
        if (i <= 0 && i >= weightedQueueList_.size()) {
            throw new IndexOutOfBoundsException();
        }
        queueIndex_ = i;
    }
}
