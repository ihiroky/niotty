package net.ihiroky.niotty.event;

import net.ihiroky.niotty.Transport;
import net.ihiroky.niotty.TransportEvent;

/**
 * Created on 13/01/11, 14:17
 *
 * @author Hiroki Itoh
 */
public class MessageEvent<E> implements TransportEvent {

    private Transport transport;
    private E message;
    private int id;

    public MessageEvent(Transport transport, E message) {
        this.transport = transport;
        this.message = message;
    }

    public MessageEvent(Transport transport, E message, int id) {
        this.transport = transport;
        this.message = message;
        this.id = id;
    }

    @Override
    public Transport getTransport() {
        return transport;
    }

    public E getMessage() {
        return message;
    }

    public int getId() {
        return id;
    }

    @Override
    public String toString() {
        return "transport:[" + transport + "], message:[" + message + ']';
    }
}
