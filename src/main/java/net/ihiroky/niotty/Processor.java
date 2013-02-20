package net.ihiroky.niotty;

/**
 * Created on 13/01/16, 17:11
 *
 * @author Hiroki Itoh
 */
public interface Processor<T extends Transport, C extends TransportConfig> {
    void start();
    void stop();
    T createTransport(C config);
}
