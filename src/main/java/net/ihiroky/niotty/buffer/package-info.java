/**
 * Defines buffers to encode an object to byte data, and decode byte data to an object.
 *
 * {@link net.ihiroky.niotty.buffer.CodecBuffer} defines the interface the buffer. The {@code CodecBuffer} provides
 * encode and decode methods for primitives and String, and methods for data transfer between {@code CodecBuffer}s.
 * {@link net.ihiroky.niotty.buffer.BufferSink} defines the interface to write data in the {@code CodecBuffer}.
 */
package net.ihiroky.niotty.buffer;
