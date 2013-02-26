package net.ihiroky.niotty.event;

import net.ihiroky.niotty.Transport;
import net.ihiroky.niotty.TransportEvent;

/**
 * Created on 13/01/11, 14:17
 *
 * @author Hiroki Itoh
 */
public class MessageEvent<E> implements TransportEvent {

    private Transport transport_;
    private E message_;

    public MessageEvent(Transport transport, E message) {
        this.transport_ = transport;
        this.message_ = message;
    }

    @Override
    public Transport getTransport() {
        return transport_;
    }

    public E getMessage() {
        return message_;
    }

    @Override
    public String toString() {
        return "transport:[" + transport_ + "], message:[" + message_ + ']';
    }
}
