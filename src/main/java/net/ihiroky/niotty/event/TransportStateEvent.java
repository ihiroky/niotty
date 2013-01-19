package net.ihiroky.niotty.event;

import net.ihiroky.niotty.Transport;
import net.ihiroky.niotty.TransportEvent;

/**
 * Created on 13/01/11, 13:47
 *
 * @author Hiroki Itoh
 */
public class TransportStateEvent implements TransportEvent {

    private Transport transport;
    private TransportState state;
    private Object value;

    public TransportStateEvent(Transport transport, TransportState state, Object value) {
        this.transport = transport;
        this.state = state;
        this.value = value;
    }

    @Override
    public Transport getTransport() {
        return transport;
    }

    public TransportState getState() {
        return state;
    }

    public Object getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "transport:[" + transport + "], state:[" + state + "], value:[" + value + ']';
    }
}
