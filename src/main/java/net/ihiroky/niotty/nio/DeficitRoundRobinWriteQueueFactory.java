package net.ihiroky.niotty.nio;

import java.util.Objects;

/**
 * Implementation of {@link net.ihiroky.niotty.nio.WriteQueueFactory}
 * to create {@link net.ihiroky.niotty.nio.DeficitRoundRobinWriteQueue}.
 * // TODO parameter explanation.
 * @author Hiroki Itoh
 */
public class DeficitRoundRobinWriteQueueFactory implements WriteQueueFactory {

    private int roundBonus_;
    private float[] weights_;

    private static final int DEFAULT_ROUND_BONUS = 1024;

    public DeficitRoundRobinWriteQueueFactory(float...weights) {
        this(DEFAULT_ROUND_BONUS, weights);
    }

    public DeficitRoundRobinWriteQueueFactory(int roundBonus, float...weights) {
        if (roundBonus <= 0) {
            throw new IllegalArgumentException("roundBonus must be positive.");
        }
        Objects.requireNonNull(weights, "weights");

        roundBonus_ = roundBonus;
        weights_ = weights.clone();
    }

    @Override
    public WriteQueue newriteQueue() {
        return new DeficitRoundRobinWriteQueue(roundBonus_, weights_);
    }
}
