package net.ihiroky.niotty;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

/**
 * @author Hiroki Itoh
 */
public class DatagramMessage<E> {

    E message_;
    SocketAddress socketAddress_;

    public DatagramMessage(E message, SocketAddress socketAddress) {
        message_ = message;
        socketAddress_ = socketAddress;
    }

    public E message() {
        return message_;
    }

    public SocketAddress socketAddress() {
        return socketAddress_;
    }

    public InetSocketAddress inetSocketAddress() {
        return (InetSocketAddress) socketAddress_;
    }
}
