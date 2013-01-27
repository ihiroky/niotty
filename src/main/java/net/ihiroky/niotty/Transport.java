package net.ihiroky.niotty;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketAddress;

/**
 * Created on 13/01/09, 17:45
 *
 * @author Hiroki Itoh
 */
public interface Transport {

    void bind(SocketAddress local);
    void connect(SocketAddress remote);
    void close();
    void join(InetAddress group, NetworkInterface networkInterface, InetAddress source);
    void write(Object message);
    void write(Object message, SocketAddress remote);
    void addListener(TransportListener listener);
    void removeListener(TransportListener listener);
}
