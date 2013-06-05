package net.ihiroky.niotty;

/**
 * @author Hiroki Itoh
 */
public interface StageContext<O> {
    StageKey key();
    Transport transport();
    TransportParameter transportParameter();
    void proceed(O output);
}
