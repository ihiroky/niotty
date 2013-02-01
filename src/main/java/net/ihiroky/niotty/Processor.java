package net.ihiroky.niotty;

/**
 * Created on 13/01/16, 17:11
 *
 * @author Hiroki Itoh
 */
public interface Processor<C extends TransportConfig> {
    void start();
    void stop();
    Transport createTransport(C config);
}
