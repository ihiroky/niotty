package net.ihiroky.niotty.codec;

import net.ihiroky.niotty.EventFuture;
import net.ihiroky.niotty.StageContext;
import net.ihiroky.niotty.StoreStage;
import net.ihiroky.niotty.Event;
import net.ihiroky.niotty.buffer.Packet;
import net.ihiroky.niotty.util.Arguments;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * <p>A implementation of {@link net.ihiroky.niotty.StoreStage} using
 * <a ref="http://en.wikipedia.org/wiki/Deficit_round_robin">deficit round robin</a> algorithm.</p>
 *
 * <p>This class has weighted queues.A weight index specified by {@code WeightedMessage}
 * decides the queue to be inserted. If the weightIndex is out of the index defined
 * by {@code weights} specified in the constructor (default index), then the {@code Packet} is used
 * as the basis of the flush calculation. If valid, then it is inserted to the weighted queue,
 * the same order as {@code weights}. If the input that is not the {@code WeightedMessage},
 * skip the round robin calculation and pass it to the next stage.</p>
 *
 * <p>The size of the inputs are checked and these content are flushed in a round.
 * A default indexed message is a basis of flush size of calculation.
 * This is immediately flushed, not queued. The flush size of the weighted queues are limited
 * by the smoothed flush size of the default message's flush size and their weight,
 * which is a rate against the smoothed flush size. The weight is in [0.05, 1].
 * The flush sizes of the weighted queues, as known as deficit counter, is accumulated
 * across the rounds. If the counters get over the size of an element in the weighted queues,
 * then the element is flushed and the counter is decremented by its actual flush size.</p>
  */
public class DeficitRoundRobinEncoder extends StoreStage {

    private final List<Deque<Pair<Packet>>> weightedQueueList_;
    private final float[] weights_;
    private final int[] deficitCounter_;
    private int smoothedBaseQuantum_;
    private int firstIndex_; // TODO calculate
    private final long timerIntervalNanos_;
    private EventFuture timerFuture_;

    private static final float MIN_WEIGHT = 0.05f;
    private static final float MAX_WEIGHT = 1f;
    private static final int SMOOTH_SHIFT = 3;
    private static final int INVALID_QUEUE_INDEX = -1;
    private static final int DEFAULT_INITIAL_BASE_QUANTUM = 1024;
    private static final long DEFAULT_TIMER_INTERVAL_MILLIS = 1L;

    public DeficitRoundRobinEncoder(float...weights) {
        this(DEFAULT_INITIAL_BASE_QUANTUM, DEFAULT_TIMER_INTERVAL_MILLIS, TimeUnit.MILLISECONDS, weights);
    }

    /**
     * Creates a instance of {@code DeficitRoundRobinEncoder}.
     * The weighed queues are created according to a specified {@code weights}.
     *
     * @param initialBaseQuantum an initial value of the smoothed flush size for the base queue
     * @param interval
     * @param timeUnit
     * @param weights an array of the weight for the weighted queues, each weight must be in [0.05, 1]
     */
    public DeficitRoundRobinEncoder(int initialBaseQuantum, long interval, TimeUnit timeUnit, float ...weights) {
        int length = weights.length;
        List<Deque<Pair<Packet>>> ql = new ArrayList<Deque<Pair<Packet>>>(length);
        for (int i = 0; i < length; i++) {
            float weight = weights[i];
            if (weight < MIN_WEIGHT || weight > MAX_WEIGHT) {
                throw new IllegalArgumentException("The weight must be in [" + MIN_WEIGHT + ", " + MAX_WEIGHT + "]");
            }
            ql.add(new ArrayDeque<Pair<Packet>>());
        }

        weightedQueueList_ = ql;
        weights_ = Arrays.copyOf(weights, length);
        timerIntervalNanos_ = timeUnit.toNanos(interval);
        deficitCounter_ = new int[length];
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
            // ps[i] = (int) Math.round(BASE_WEIGHT * (double) weights[i]);
        }
        return ps;
    }

    @Override
    public void stored(StageContext context, Object message, Object parameter) {

        if (!(message instanceof WeightedMessage)) {
            context.proceed(message, parameter);
            return;
        }

        WeightedMessage pm = (WeightedMessage) message;
        Packet data = (Packet) pm.message();
        int index = pm.weightIndex();
        if (index < 0 || index >= weights_.length) {
            smoothedBaseQuantum_ = smooth(smoothedBaseQuantum_, data.remaining());
            context.proceed(data, parameter);
            flush(context, smoothedBaseQuantum_);
        } else {
            weightedQueueList_.get(index).offer(new Pair<Packet>(data, parameter));
            flush(context, 0);
        }
    }

    @Override
    public void exceptionCaught(StageContext context, Exception exception) {
    }

    @Override
    public void activated(StageContext context) {
    }

    @Override
    public void deactivated(StageContext context) {
        for (Deque<Pair<Packet>> q : weightedQueueList_) {
            while (!q.isEmpty()) {
                Pair<Packet> e = q.poll();
                context.proceed(e.message_, e.parameter_);
            }
        }
    }

    @Override
    public void eventTriggered(StageContext context, Object event) {
    }

    private void flush(final StageContext context, int baseQuantum) {
        int size = weightedQueueList_.size();
        int firstIndex = INVALID_QUEUE_INDEX;
        for (int i = 0; i < size; i++) {
            Deque<Pair<Packet>> q = weightedQueueList_.get(i);
            // flush operation
            int deficitCounter = deficitCounter_[i] + (int) (baseQuantum * weights_[i]);
            if (!q.isEmpty()) {
                int remaining = q.peek().message_.remaining();
                while (deficitCounter >= remaining) { // always true if remaining == 0
                    Pair<Packet> e = q.poll();
                    context.proceed(e.message_, e.parameter_);
                    deficitCounter -= remaining;
                }
                deficitCounter_[i] = deficitCounter;
                if (firstIndex == INVALID_QUEUE_INDEX && !q.isEmpty()) {
                    firstIndex = i;
                }
            } else {
                deficitCounter_[i] = Math.min(deficitCounter, smoothedBaseQuantum_ << 1);
            }
        }

        firstIndex_ = firstIndex;
        if (firstIndex != INVALID_QUEUE_INDEX) {
            if (timerFuture_ != null) {
                return;
            }
            timerFuture_ = context.schedule(new Event() {
                @Override
                public long execute(TimeUnit timeUnit) throws Exception {
                    int baseQuantum = smoothedBaseQuantum_;
                    if (baseQuantum == 0) {
                        Deque<Pair<Packet>> q = weightedQueueList_.get(firstIndex_);
                        baseQuantum = (int) (q.peek().message_.remaining() / weights_[firstIndex_]);
                    }
                    flush(context, baseQuantum);
                    return (firstIndex_ == INVALID_QUEUE_INDEX)
                            ? DONE : timeUnit.convert(timerIntervalNanos_, TimeUnit.NANOSECONDS);
                }
            }, timerIntervalNanos_, TimeUnit.NANOSECONDS);
        } else {
            if (timerFuture_ != null) {
                timerFuture_.cancel();
                timerFuture_ = null;
            }
        }
    }

    int smoothedBaseQuantum() {
        return smoothedBaseQuantum_;
    }

    int numberOfQueues() {
        return weights_.length;
    }

    int deficitCounter(int priority) {
        return deficitCounter_[priority];
    }

    Deque<Pair<Packet>> queue(int priority) {
        return weightedQueueList_.get(priority);
    }

    EventFuture timerFuture() {
        return timerFuture_;
    }

    private static int smooth(int oldValue, int newValue) {
        // The same as oldValue * 7/8 + newValue * 1/8
        return oldValue + ((newValue - oldValue) >> SMOOTH_SHIFT);
    }

}
