/**
 * Provides Transport and I/O thread implementation using NIO.
 *
 * <h3>Selector and its pool</h3>
 * This package has three type of NIO selector separated by its role;
 * accept, connect and message I/O. The message I/O means read and write.
 * These processes its own selection and asynchronous task received
 * through a task queue. The selector is associated with a dedicated
 * thread to execute its selection. The thread and the task queue
 * is provided by {@link net.ihiroky.niotty.TaskLoop}.
 *
 * <h3>Transport implementation</h3>
 * {@link net.ihiroky.niotty.Transport} implementation of Server side TCP socket is
 * {@link net.ihiroky.niotty.nio.NioServerSocketTransport}, which is created by
 * {@link net.ihiroky.niotty.nio.NioServerSocketProcessor}. Client side one is
 * {@link net.ihiroky.niotty.nio.NioClientSocketTransport}, which is created by
 * {@link net.ihiroky.niotty.nio.NioClientSocketProcessor}. These subclass of
 * {@link net.ihiroky.niotty.Processor} manages also the selector pool described
 * above.
 */
package net.ihiroky.niotty.nio;
