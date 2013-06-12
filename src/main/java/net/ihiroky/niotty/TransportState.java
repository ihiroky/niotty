package net.ihiroky.niotty;

/**
 * Changes in state of {@code Transport} which asynchronously executed.
 *
 * @author Hiroki Itoh
 */
public enum TransportState {
    /** The state to connect. */
    CONNECTED,

    /** The state to change interest set. */
    INTEREST_OPS,

    /** The state to close the transport. */
    CLOSED,

    /** The state to disconnect. */
    DISCONNECT,

    SHUTDOWN_OUTPUT,
    SHUTDOWN_INPUT,
}
