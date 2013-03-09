package net.ihiroky.niotty;

/**
 * Created on 13/01/11, 13:47
 *
 * @author Hiroki Itoh
 */
public class TransportStateEvent {

    private TransportState state_;
    private Object value_;
    private DefaultTransportFuture future_;

    public TransportStateEvent(TransportState state, Object value) {
        this(state, value, null);
    }

    public TransportStateEvent(TransportState state, Object value, DefaultTransportFuture future) {
        future_ = future;
        state_ = state;
        value_ = value;
    }

    public TransportState state() {
        return state_;
    }

    public Object value() {
        return value_;
    }

    public DefaultTransportFuture future() {
        return future_;
    }

    @Override
    public String toString() {
        return "state:[" + state_ + "], value:[" + value_ + ']';
    }
}
