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
}
