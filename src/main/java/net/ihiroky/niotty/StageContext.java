package net.ihiroky.niotty;

/**
 * @author Hiroki Itoh
 */
public interface StageContext<O> {
    StageKey key();
    Transport transport();
    void proceed(O output);
    void proceed(TransportStateEvent event);
}
