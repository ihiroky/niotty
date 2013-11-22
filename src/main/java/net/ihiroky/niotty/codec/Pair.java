package net.ihiroky.niotty.codec;

/**
 * A container class which holds a message and its parameter.
 * @param <M> the type of the message
 */
class Pair<M> {
    final M message_;
    final Object parameter_;

    Pair(M message, Object parameter) {
        message_ = message;
        parameter_ = parameter;
    }
}
