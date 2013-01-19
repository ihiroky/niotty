package net.ihiroky.niotty.event;

/**
 * Created on 13/01/11, 13:52
 *
 * @author Hiroki Itoh
 */
public enum TransportState {
    OPEN,
    BOUND,
    CONNECTED,
    ACCEPTED,
    INTEREST_OPS,
    STALE,
    RECOVERED,
    ;
}
