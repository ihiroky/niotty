package net.ihiroky.niotty.codec;

import net.ihiroky.niotty.util.Arguments;

/**
 *
 */
public class WeightedMessage {
    private Object message_;
    private final int weightIndex_;

    private static final int DEFAULT_INDEX = -1;

    public WeightedMessage(Object message) {
        message_ = Arguments.requireNonNull(message, "message");
        weightIndex_ = DEFAULT_INDEX;
    }

    public WeightedMessage(Object message, int weightIndex) {
        message_ = Arguments.requireNonNull(message, "message");
        weightIndex_ = weightIndex;
    }

    public Object message() {
        return message_;
    }

    public void setMessage(Object message) {
        message_ = Arguments.requireNonNull(message, "message");
    }

    public int weightIndex() {
        return weightIndex_;
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof WeightedMessage) {
            WeightedMessage that = (WeightedMessage) object;
            return this.message_.equals(that.message_) && this.weightIndex_ == that.weightIndex_;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return message_.hashCode() + weightIndex_;
    }

    @Override
    public String toString() {
        return "message:" + message_ + ", weightIndex:" + weightIndex_;
    }
}
