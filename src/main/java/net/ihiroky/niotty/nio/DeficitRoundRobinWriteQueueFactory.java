package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.util.Arguments;

/**
 * Implementation of {@link WriteQueueFactory}
 * to create {@link DeficitRoundRobinWriteQueue}.
 *
 * This class requires two kind of parameter, {@code baseQueueLimit} and {@code weights}.
 * The {@code baseQueueLimit} is a byte length to limit the flush size of {@code DeficitRoundRobinWriteQueue}'s
 * base queue at one time. The {@code weights} is flush limit rates of queues other than the base queue.
 *
 * @see net.ihiroky.niotty.nio.DeficitRoundRobinWriteQueue
 * @author Hiroki Itoh
 */
public class DeficitRoundRobinWriteQueueFactory implements WriteQueueFactory {

    private final int initialBaseQuantum_;
    private final float[] weights_;

    private static final int DEFAULT_BASE_ROUND_LIMIT = 4096;

    /**
     * Creates the factory of {@link DeficitRoundRobinWriteQueue}.
     *
     * @param weights flush size weights of the queues other than the base queue
     */
    public DeficitRoundRobinWriteQueueFactory(float...weights) {
        this(DEFAULT_BASE_ROUND_LIMIT, weights);
    }

    /**
     * Creates the factory of {@link DeficitRoundRobinWriteQueue}.
     *
     * @param initialBaseQuantum an initial value of the smoothed flush size for the base queue
     * @param weights flush size weights of the queues other than the base queue
     */
    public DeficitRoundRobinWriteQueueFactory(int initialBaseQuantum, float...weights) {
        initialBaseQuantum_ = Arguments.requirePositive(initialBaseQuantum, "initialBaseQuantum");
        weights_ = Arguments.requireNonNull(weights, "weights").clone();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WriteQueue newWriteQueue() {
        return new DeficitRoundRobinWriteQueue(initialBaseQuantum_, weights_);
    }
}
