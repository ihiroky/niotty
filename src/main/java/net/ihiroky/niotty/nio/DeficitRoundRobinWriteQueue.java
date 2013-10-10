package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.TransportParameter;
import net.ihiroky.niotty.buffer.BufferSink;
import net.ihiroky.niotty.util.Arguments;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.GatheringByteChannel;
import java.util.ArrayList;
import java.util.List;

/**
 * A implementation of {@link WriteQueue} using
 * <a ref="http://en.wikipedia.org/wiki/Deficit_round_robin">deficit round robin</a> algorithm.
 *
 * This class has a base queue and weighted queues. The all queues are {@link SimpleWriteQueue}.
 * They are checked and flushed in a round which is executed by calling
 * {@link #flushTo(java.nio.channels.GatheringByteChannel)} or
 * {@link #flushTo(java.nio.channels.DatagramChannel, java.nio.ByteBuffer)}.
 * The base queue is a basis of flush size of calculation. The flush size limit of the base queue is
 * {@code Integer.MAX_VALUE}. If some packets ({@link net.ihiroky.niotty.buffer.BufferSink})
 * are in the base queue, a flush operation for the base queue is executed. The basis of flush size
 * is a result of the operation. On the other hand, the flush size of the weighted queues are limited
 * by the smoothed flush size of the base queue and their weight, which is a rate against the smoothed flush size.
 * The weight is in [5%, 100%]. The flush sizes of the weighted queues, as known as deficit counter, is accumulated
 * across the rounds. If the counters get over the size of an element in the weighted queues, then the element is
 * flushed and the counter is decremented by its actual flush size.
 *
 * A priority of the {@link net.ihiroky.niotty.buffer.BufferSink} passed at
 * {@link #offer(AttachedMessage)} decides the queue to be inserted.
 * If the priority is negative, then the {@code BufferSink} is inserted to the base queue. If positive, then it
 * is inserted to the weighted queue, the same order as {@code weights} specified by the constructor.
 *
 */
public class DeficitRoundRobinWriteQueue implements WriteQueue {

    private final SimpleWriteQueue baseQueue_;
    private final List<SimpleWriteQueue> weightedQueueList_;
    private final int[] weights_;
    private final int[] deficitCounter_;
    private int queueIndex_;
    private int lastFlushedBytes_;
    private int smoothedBaseQuantum_;

    private static final int BASE_WEIGHT = 100;
    private static final int QUEUE_INDEX_BASE = -1;
    private static final float MIN_WEIGHT = 0.05f;
    private static final float MAX_WEIGHT = 1f;
    private static final int SMOOTH_SHIFT = 3;
    private static final int QUOTER_BY_SHIFT = 2;
    private static final int MIN_BASE_QUANTUM = 1;

    /**
     * Creates a instance of {@code DeficitRoundRobinWriteQueue}.
     * The weighed queues are created according to a specified {@code weights}.
     *
     * @param initialBaseQuantum an initial value of the smoothed flush size for the base queue
     * @param weights an array of the weight for the weighted queues, each weight must be in [0.05, 1]
     */
    DeficitRoundRobinWriteQueue(int initialBaseQuantum, float ...weights) {
        int length = weights.length;
        List<SimpleWriteQueue> ql = new ArrayList<SimpleWriteQueue>(length);
        for (int i = 0; i < length; i++) {
            ql.add(new SimpleWriteQueue());
        }
        baseQueue_ = new SimpleWriteQueue();
        weightedQueueList_ = ql;
        weights_ = convertWeightsRateToPercent(weights);
        deficitCounter_ = new int[length];
        queueIndex_ = QUEUE_INDEX_BASE;
        smoothedBaseQuantum_ = Arguments.requirePositive(initialBaseQuantum, "initialBaseQuantum");
    }

    /**
     * Converts specified {@code weights} unit from rate to percent.
     *
     * @param weights array of the weight which is rate against the base queue, each elements must be in [0.05, 1].
     * @return array of the weight, which is percent
     * @throws IllegalArgumentException if the element of {@code weights} is not in [0.05, 1],
     *    or it is possible that the result of the flush size calculation overflows
     */
    private static int[] convertWeightsRateToPercent(float... weights) {
        int length = weights.length;
        int[] ps = new int[length];
        for (int i = 0; i < length; i++) {
            float weight = weights[i];
            if (weight < MIN_WEIGHT || weight > MAX_WEIGHT) {
                throw new IllegalArgumentException("The weight must be in [" + MIN_WEIGHT + ", " + MAX_WEIGHT + "]");
            }
            ps[i] = (int) Math.round(BASE_WEIGHT * (double) weights[i]);
        }
        return ps;
    }

    @Override
    public boolean offer(AttachedMessage<BufferSink> message) {
        TransportParameter p = message.parameter();
        if (p.priority() < 0) {
            return baseQueue_.offer(message);
        }

        int priority = p.priority();
        if (priority >= weights_.length) {
            throw new IllegalStateException("unsupported priority:" + p.priority());
        }
        return weightedQueueList_.get(priority).offer(message);
    }

    @Override
    public FlushStatus flushTo(GatheringByteChannel channel) throws IOException {
        return flushTo(new GatheringDelegate(channel), 0);
    }

    @Override
    public FlushStatus flushTo(DatagramChannel channel, ByteBuffer writeBuffer) throws IOException {
        return flushTo(new DatagramDelegate(channel, writeBuffer), 0);
    }

    private FlushStatus flushTo(Delegate delegate, int previousFlushedByte) throws IOException {
        int baseQuantum = 0;
        int flushedBytes = previousFlushedByte;
        int queueIndex = queueIndex_;
        if (queueIndex == QUEUE_INDEX_BASE) {
            FlushStatus status = delegate.flush(baseQueue_, Integer.MAX_VALUE);
            baseQuantum = baseQueue_.lastFlushedBytes();
            if (baseQuantum > 0) {
                smoothedBaseQuantum_ = smooth(smoothedBaseQuantum_, baseQuantum);
                flushedBytes += baseQuantum;
            }
            if (status == FlushStatus.FLUSHING) {
                countUpDeficitCounters(baseQuantum, 0, weightedQueueList_.size());
                lastFlushedBytes_ = flushedBytes;
                return status;
            }
            queueIndex = 0;
        }
        if (baseQuantum == 0) {
            int quoterOfSbq = smoothedBaseQuantum_ >> QUOTER_BY_SHIFT;
            baseQuantum = (quoterOfSbq > 0) ? quoterOfSbq : MIN_BASE_QUANTUM;
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
            FlushStatus status = delegate.flush(q, newDeficitCounter);
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
            } else if (status == FlushStatus.SKIPPED) {
                existsSkipped = true;
            }
        }
        lastFlushedBytes_ = flushedBytes;

        // finish if baseQueue_ is already checked.
        if (queueIndex_ == QUEUE_INDEX_BASE) {
            return existsSkipped ? FlushStatus.SKIPPED : FlushStatus.FLUSHED;
        }

        // one more round if this method starts in queueList.
        queueIndex_ = QUEUE_INDEX_BASE;
        return baseQueue_.isEmpty()
                ? (existsSkipped ? FlushStatus.SKIPPED : FlushStatus.FLUSHED)
                : flushTo(delegate, flushedBytes);
    }

    private static int smooth(int oldValue, int newValue) {
        // The same as oldValue * 7/8 + newValue * 1/8
        return oldValue + ((newValue - oldValue) >> SMOOTH_SHIFT);
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

    int smoothedBaseQuantum() {
        return smoothedBaseQuantum_;
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

    private interface Delegate {
        FlushStatus flush(SimpleWriteQueue queue, int bytes) throws IOException;
    }

    private static class GatheringDelegate implements Delegate {

        private GatheringByteChannel channel_;

        GatheringDelegate(GatheringByteChannel channel) {
            channel_ = channel;
        }

        @Override
        public FlushStatus flush(SimpleWriteQueue queue, int bytes) throws IOException {
            return queue.flushTo(channel_, bytes);
        }
    }

    private static class DatagramDelegate implements Delegate {

        private DatagramChannel channel_;
        private ByteBuffer writeBuffer_;

        DatagramDelegate(DatagramChannel channel, ByteBuffer writeBuffer) {
            channel_ = channel;
            writeBuffer_ = writeBuffer;
        }

        @Override
        public FlushStatus flush(SimpleWriteQueue queue, int bytes) throws IOException {
            return queue.flushTo(channel_, writeBuffer_, bytes);
        }
    }
}
