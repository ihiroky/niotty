package net.ihiroky.niotty;

import java.util.Objects;

/**
 * Created on 13/01/11, 13:47
 *
 * @author Hiroki Itoh
 */
public abstract class TransportStateEvent {

    private TransportState state_;
    private Object value_;

    public TransportStateEvent(TransportState state) {
        this(state, null);
    }

    public TransportStateEvent(TransportState state, Object value) {
        Objects.requireNonNull(state, "state");
        state_ = state;
        value_ = value;
    }

    public TransportState state() {
        return state_;
    }

    public Object value() {
        return value_;
    }

    protected void setValue(Object value) {
        value_ = value;
    }

    @Override
    public String toString() {
        return state_.toString();
    }

    public abstract void execute();
}
