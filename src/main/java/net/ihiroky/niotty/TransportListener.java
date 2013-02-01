package net.ihiroky.niotty;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketAddress;

/**
 * @author Hiroki Itoh
 */
public interface TransportListener {

    void onBind(Transport transport, SocketAddress localAddress);
    void onConnect(Transport transport, SocketAddress remoteAddress);
    void onJoin(Transport transport, InetAddress group, NetworkInterface networkInterface, InetAddress source);
    void onClose(Transport transport);
}
