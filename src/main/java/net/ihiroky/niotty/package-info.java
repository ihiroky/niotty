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
 * {@link net.ihiroky.niotty.LoadPipeline} which consists of {@link net.ihiroky.niotty.LoadStage}s.
 * User defined {@code LoadStage}s may be included to receive the message.
 * The write operation is invoked in user codes through the {@code Transport}.
 * The write message is processed by the {@link net.ihiroky.niotty.StorePipeline}
 * which consists of {@link net.ihiroky.niotty.StoreStage}s. At last, the message
 * is passed into the I/O threads. The other operations may be asynchronous
 * depending on the {@code Transport} implementation.
 * <p></p>
 * The pipelines processes not only messages but also {@link net.ihiroky.niotty.TransportStateEvent}.
 * This has state change of the {@code Transport}. If the stage of the {@code Transport}
 * is changed in I/O thread, it invokes {@code LoadPipeline} processing to notify
 * the change to user code. If an user wants to change the {@code Transport} state
 * (ex. close operation) the {@code TransportStateEvent} is processed through
 * {@code StorePipeline} and passes into the I/O thread to change the state.
 *
 * <h3>TaskLoop and TaskLoopGroup</h3>
 * {@link net.ihiroky.niotty.TaskLoop} consists of a queue to receive
 * {@link net.ihiroky.niotty.Task} and a thread to execute the {@code Task}
 * and its own processing implemented by its sub type.
 * {@link net.ihiroky.niotty.TaskLoopGroup} manages the {@code TaskLoop} instances
 * and registers objects which are processed by the {@code TaskLoop} with it.
 * Once the object is registered with the object, it is processed by the
 * thread {@code TaskLoop} until unregistered.
 *
 * <h3>Processor</h3>
 * {@link net.ihiroky.niotty.Processor} manages the {@code Transport} and the I/O threads.
 * To create an instance of {@code Transport}, use {@code Transport} factory method of {@code Processor}.
 */
package net.ihiroky.niotty;
