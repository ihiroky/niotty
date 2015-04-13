package net.ihiroky.niotty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public abstract class ExponentialBackOffEvent implements Event {

    private long nextTimeoutNanos_;
    private int trialCount_;
    private final int maxTrialCount_;
    private final String name_;

    private static final int CANCELLED = Integer.MAX_VALUE;
    private static final int CANCELLED_NEXT = Integer.MIN_VALUE;
    private static final Logger LOG = LoggerFactory.getLogger(ExponentialBackOffEvent.class);

    ExponentialBackOffEvent(String name, long nextTimeoutNanos, int maxTrialCount) {
        if (maxTrialCount < 0) {
            throw new IllegalArgumentException("The maxTrialCount must not be negative.");
        }

        nextTimeoutNanos_ = nextTimeoutNanos;
        maxTrialCount_ = maxTrialCount;
        name_ = name;
    }

    @Override
    public long execute() throws Exception {
        if (trialCount_++ >= maxTrialCount_) {
            if (trialCount_ == CANCELLED_NEXT) {
                LOG.debug("[execute] Cancelled: {}", name_);
            } else {
                LOG.debug("[execute] Reaches max trial count: {}", name_);
                reachMaxTrialCount();
            }
            return Event.DONE;
        }

        if (perform()) {
            LOG.debug("[execute] Finished {}. Trial count: {}", name_, trialCount_);
            return Event.DONE;
        }

        long timeout = nextTimeoutNanos_;
        nextTimeoutNanos_ += timeout;
        if (nextTimeoutNanos_ < 0) {
            nextTimeoutNanos_ = Long.MAX_VALUE;
        }
        return timeout;
    }

    boolean cancel() {
        if (trialCount_ == 0) {
            trialCount_ = CANCELLED;
            return true;
        }
        return false;
    }

    long getNextTimeoutNanos() {
        return nextTimeoutNanos_;
    }

    int getMaxTrialCount() {
        return maxTrialCount_;
    }

    String getName() {
        return name_;
    }

    abstract boolean perform();
    abstract void reachMaxTrialCount();
}
