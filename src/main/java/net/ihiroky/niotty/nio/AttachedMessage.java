package net.ihiroky.niotty.nio;

/**
 * An message that an object is attached to.
 *
 * @param <I> the type of the message.
 */
public class AttachedMessage<I> {

    private final I message_;
    private final Object parameter_;

    public AttachedMessage(I message) {
        this(message, null);
    }

    public AttachedMessage(I message, Object parameter) {
        message_ = message;
        parameter_ = parameter;
    }

    public I message() {
        return message_;
    }

    public Object parameter() {
        return parameter_;
    }

    @Override
    public String toString() {
        return "message: (" + message_ + "), parameter: (" + parameter_ + ")";
    }
}
