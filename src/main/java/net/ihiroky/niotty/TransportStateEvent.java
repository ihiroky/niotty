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
    public int hashCode() {
        return Objects.hash(state_, value_);
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof TransportStateEvent) {
            TransportStateEvent that = (TransportStateEvent) object;
            boolean stateEqual = this.state_ == that.state_;
            boolean valueEqual = (this.value_ != null) ? this.value_.equals(that.value_) : that.value_ == null;
            return stateEqual && valueEqual;
        }
        return false;
    }

    @Override
    public String toString() {
        return "(state: " + state_ + ", value: " + value_ + ')';
    }

    public abstract void execute();
}
