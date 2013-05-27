package net.ihiroky.niotty;

/**
 * @author Hiroki Itoh
 */
public interface StageContext<O> {
    StageKey key();
    Transport transport();
    Object attachment();
    void proceed(O output);
    void proceed(TransportStateEvent event);
}
