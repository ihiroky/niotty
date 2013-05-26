package net.ihiroky.niotty;

import java.net.SocketAddress;

/**
 * @author Hiroki Itoh
 */
public interface TransportListener {

    void onAccept(Transport transport, SocketAddress remoteAddress);
    void onConnect(Transport transport, SocketAddress remoteAddress);
    void onClose(Transport transport);
}
