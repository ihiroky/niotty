package net.ihiroky.niotty;

import java.net.SocketAddress;

/**
 * Created on 13/01/09, 17:45
 *
 * @author Hiroki Itoh
 */
public interface Transport {

    TransportFuture bind(SocketAddress local);
    TransportFuture connect(SocketAddress remote);
    TransportFuture close();
    void write(Object message);
    void write(Object message, TransportParameter parameter);
    void addListener(TransportListener listener);
    void removeListener(TransportListener listener);
    SocketAddress localAddress();
    SocketAddress remoteAddress();
    boolean isOpen();
    Object attach(Object attachment);
    Object attachment();
    int pendingWriteBuffers();
}
