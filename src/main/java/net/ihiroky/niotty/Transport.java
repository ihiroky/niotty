package net.ihiroky.niotty;

import java.net.SocketAddress;

/**
 * Created on 13/01/09, 17:45
 *
 * @author Hiroki Itoh
 */
public interface Transport {

    void bind(SocketAddress localAddress);
    void connect(SocketAddress remoteAddress);
    void close();
    void write(Object message);
}
