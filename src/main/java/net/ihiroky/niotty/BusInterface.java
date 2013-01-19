package net.ihiroky.niotty;

/**
 * Created on 13/01/09, 18:26
 *
 * @author Hiroki Itoh
 */
public interface BusInterface<C extends TransportConfig> {

    Transport createTransport();
    C getConfig();
    void start();
    void stop();
}
