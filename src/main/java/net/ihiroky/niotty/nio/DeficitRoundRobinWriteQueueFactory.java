package net.ihiroky.niotty.nio;

import java.util.Objects;

/**
 * Implementation of {@link net.ihiroky.niotty.nio.WriteQueueFactory}
 * to create {@link net.ihiroky.niotty.nio.DeficitRoundRobinWriteQueue}.
 *
 * This class requires two kind of parameter, {@code baseQueueLimit} and {@code weights}.
 * The {@code baseQueueLimit} is a byte length to limit the flush size of {@code DeficitRoundRobinWriteQueue}'s
 * base queue at one time. The {@code weights} is flush limit rates of queues other than the base queue.
 *
 * @see net.ihiroky.niotty.nio.DeficitRoundRobinWriteQueue
 * @author Hiroki Itoh
 */
public class DeficitRoundRobinWriteQueueFactory implements WriteQueueFactory {

    private int baseQueueLimit_;
    private int[] weights_;

    private static final int DEFAULT_BASE_ROUND_LIMIT = 4096;

    /**
     * Creates the factory of {@link net.ihiroky.niotty.nio.DeficitRoundRobinWriteQueue}.
     *
     * @param weights flush size weights of the queues other than the base queue
     */
    public DeficitRoundRobinWriteQueueFactory(float...weights) {
        this(DEFAULT_BASE_ROUND_LIMIT, weights);
    }

    /**
     * Creates the factory of {@link net.ihiroky.niotty.nio.DeficitRoundRobinWriteQueue}.
     *
     * @param baseQueueLimit a byte length to limit the flush size of the base queue at one time
     * @param weights flush size weights of the queues other than the base queue
     */
    public DeficitRoundRobinWriteQueueFactory(int baseQueueLimit, float...weights) {
        if (baseQueueLimit <= 0) {
            throw new IllegalArgumentException("baseQueueLimit must be positive.");
        }
        Objects.requireNonNull(weights, "weights");

        baseQueueLimit_ = baseQueueLimit;
        weights_ = DeficitRoundRobinWriteQueue.convertWeightsRateToPercent(baseQueueLimit, weights);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WriteQueue newWriteQueue() {
        return new DeficitRoundRobinWriteQueue(baseQueueLimit_, weights_);
    }
}
