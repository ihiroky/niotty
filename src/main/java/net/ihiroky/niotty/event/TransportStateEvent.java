package net.ihiroky.niotty.event;

import net.ihiroky.niotty.DefaultTransportFuture;
import net.ihiroky.niotty.Transport;
import net.ihiroky.niotty.TransportEvent;

/**
 * Created on 13/01/11, 13:47
 *
 * @author Hiroki Itoh
 */
public class TransportStateEvent implements TransportEvent {

    private Transport transport_;
    private DefaultTransportFuture future_;
    private TransportState state_;
    private Object value_;

    public TransportStateEvent(Transport transport, TransportState state, Object value) {
        this(transport, state, null, value);
    }

    public TransportStateEvent(Transport transport, TransportState state,
                               DefaultTransportFuture future, Object value) {
        this.transport_ = transport;
        this.future_ = future;
        this.state_ = state;
        this.value_ = value;
    }

    @Override
    public Transport getTransport() {
        return transport_;
    }

    public TransportState getState() {
        return state_;
    }

    public DefaultTransportFuture getFuture() {
        return future_;
    }

    public Object getValue() {
        return value_;
    }

    @Override
    public String toString() {
        return "transport:[" + transport_ + "], state:[" + state_ + "], value:[" + value_ + ']';
    }
}
