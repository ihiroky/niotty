package net.ihiroky.niotty;

/**
 * An message that an object is attached to.
 *
 * @param <I> the type of the message.
 * @author Hiroki Itoh
 */
public class AttachedMessage<I> {

    private final I message_;
    private final TransportParameter parameter_;

    public AttachedMessage(I message) {
        this(message, DefaultTransportParameter.NO_PARAMETER);
    }

    public AttachedMessage(I message, TransportParameter parameter) {
        message_ = message;
        parameter_ = (parameter != null) ? parameter : DefaultTransportParameter.NO_PARAMETER;
    }

    public I message() {
        return message_;
    }

    // TODO check caller if handle correctly.
    public TransportParameter parameter() {
        return parameter_;
    }

    @Override
    public String toString() {
        return "message: (" + message_ + "), parameter: (" + parameter_ + ")";
    }
}
