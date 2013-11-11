package net.ihiroky.niotty;

/**
 * Changes in state of {@code Transport} which asynchronously executed.
 *
 */
public enum TransportState {
    /** The state to bind. */
    BOUND,

    /** The state to connect. */
    CONNECTED,

    /** The state to change interest set. */
    INTEREST_OPS,

    /** The state to close the transport. */
    CLOSED,

    /** The state to disconnect. */
    DISCONNECT,

    /** The state to shutdown output. */
    SHUTDOWN_OUTPUT,

    /** The state to shutdown output. */
    SHUTDOWN_INPUT,

    JOIN,

    LEAVE,

    BLOCK,

    UNBLOCK,
}
