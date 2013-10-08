package net.ihiroky.niotty;

import java.net.SocketAddress;
import java.util.Set;

/**
 * <p>A nexus for network I/O operations.</p>
 *
 * <p>This class has a load (inbound) and store (outbound) pipeline, an attachment reference and an event listener.
 * The pipelines handles I/O request associated with this transport. The load pipeline handles user request
 * for a transport implementation. The store pipeline handles events which come up in an I/O thread.</p>
 *
 * @author Hiroki Itoh
 */
public interface Transport extends TaskSelection {

    /**
     * Binds the socket of this transport to a local address.
     * @param local The local address
     * @return a future object to get the result of this operation
     */
    TransportFuture bind(SocketAddress local);

    /**
     * Connects the  socket of this transport to a remote address.
     * @param local The local address
     * @return a future object to get the result of this operation
     */
    TransportFuture connect(SocketAddress local);

    /**
     * Closes this transport.
     * A close request comes up to the store pipeline, and then an implementation of this transport is closed.
     *
     * @return a future object to get the result of this operation
     */
    TransportFuture close();

    /**
     * Writes a specified message to this transport.
     * The message is passed to the store pipeline. If operations in the I/O thread associated with this call
     * is failed, then a close request comes up to the store pipeline in the I/O thread.
     *
     *  @param message the message
     */
    void write(Object message);

    /**
     * Writes a specified message to this transport with a specified parameter.
     * The message is passed to the store pipeline. If operations in the I/O thread associated with this call
     * is failed, then a close request comes up to the store pipeline in the I/O thread.
     *
     * @param message the message
     * @param parameter the parameter
     */
    void write(Object message, TransportParameter parameter);

    /**
     * Returns the future which represents an asynchronous close operation.
     * @return the future
     */
    TransportFuture closeFuture();

    /**
     * Returns the local address that the socket of this transport is bound to.
     * @return The local address, or null if this transport is not bound
     */
    SocketAddress localAddress();

    /**
     * Returns the remote address to which the socket of this transport is connected to.
     * @return The remote address, or null if this transport is not connected
     */
    SocketAddress remoteAddress();

    /**
     * Tells whether or not this channel is open.
     * @return true if, and only if, this channel is open
     */
    boolean isOpen();

    <T> Transport setOption(TransportOption<T> option, T value);

    <T> T option(TransportOption<T> option);

    Set<TransportOption<?>> supportedOptions();

    /**
     * Attaches the specified attachment to this transport.
     * @param attachment the attachment
     * @return the attached object
     */
    Object attach(Object attachment);

    /**
     * Returns the attachment.
     * @return the attachment
     */
    Object attachment();

    /**
     * Returns the load pipeline.
     * @return the load pipeline
     */
    LoadPipeline loadPipeline();

    /**
     * Returns the store pipeline.
     * @return the store pipeline
     */
    StorePipeline storePipeline();
}
