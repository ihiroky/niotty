package net.ihiroky.niotty;

/**
 * The state transmitted by
 * {@link net.ihiroky.niotty.Stage#deactivated(net.ihiroky.niotty.StageContext, net.ihiroky.niotty.DeactivateState)}.
 *
 */
public enum DeactivateState {
    /**
     * The state when {@link net.ihiroky.niotty.nio.NioClientSocketTransport#shutdownOutput()} is called.
     */
    STORE,

    /**
     * The state when {@link net.ihiroky.niotty.nio.NioClientSocketTransport#shutdownOutput()} is called
     * or a read operation of sockets returns -1;
     */
    LOAD,

    /**
     * The state when a transport is closed.
     */
    WHOLE,
}
