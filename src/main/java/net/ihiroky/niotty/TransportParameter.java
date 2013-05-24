package net.ihiroky.niotty;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

/**
 * @author Hiroki Itoh
 */
public interface TransportParameter {
    SocketAddress socketAddress();
    InetSocketAddress inetSocketAddress();
    int priority();
}
