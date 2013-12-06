/**
 * Provides the core API to handle I/O operation asynchronously.
 *
 * <h3>Transport, Stage and Pipeline</h3>
 * {@link net.ihiroky.niotty.Transport} is the abstraction of I/O operations
 * such as read, write, connect, close and etc. The {@code Transport} receives
 * and sends message (user data) through {@link net.ihiroky.niotty.Pipeline}
 * and passes the processed (serialized) data to the I/O thread associated with
 * the {@code Transport}. The read and write are asynchronous.
 * The read operation is invoked in the dedicated I/O threads associated with
 * the {@code Transport}. The read message is processed by the
 * {@link net.ihiroky.niotty.Pipeline} which consists of {@link net.ihiroky.niotty.Stage}s.
 * User defined {@code Stage}s may be included to receive the message.
 * The write operation is invoked in user codes through the {@code Transport}.
 * The write message is processed by the {@link net.ihiroky.niotty.Pipeline}
 * which consists of {@link net.ihiroky.niotty.Stage}s. At last, the message
 * is passed into the I/O threads. The other operations may be asynchronous
 * depending on the {@code Transport} implementation.
 * <p></p>
 * The pipelines processes not only messages but also the transport state.
 * If the state of the {@code Transport} is changed, listener methods
 * in {@link net.ihiroky.niotty.Stage} is called in the I/O threads;
 * {@link net.ihiroky.niotty.Stage#activated(net.ihiroky.niotty.StageContext)}
 * (when the transport gets readable),
 * {@link net.ihiroky.niotty.Stage#deactivated(net.ihiroky.niotty.StageContext, DeactivateState)}
 * (when the transport gets closed),
 * {@link net.ihiroky.niotty.Stage#exceptionCaught(net.ihiroky.niotty.StageContext, Exception)}
 * (when an exception occurs on I/O operation).
 *
 * <h3>EventDispatcher and EventDispatcherGroup</h3>
 * {@link net.ihiroky.niotty.EventDispatcher} consists of a queue to receive
 * {@link net.ihiroky.niotty.Event} and a thread to execute the {@code Event}
 * and its own processing implemented by its sub type.
 * {@link net.ihiroky.niotty.EventDispatcherGroup} manages the {@code EventDispatcher} instances
 * and registers objects which are processed by the {@code EventDispatcher} with it.
 * Once the object is registered with the object, it is processed by the
 * thread {@code EventDispatcher} until unregistered.
 *
 * <h3>Processor</h3>
 * {@link net.ihiroky.niotty.Processor} manages the {@code Transport} and the I/O threads.
 * To create an instance of {@code Transport}, use {@code Transport} factory method of {@code Processor}.
 */
package net.ihiroky.niotty;
