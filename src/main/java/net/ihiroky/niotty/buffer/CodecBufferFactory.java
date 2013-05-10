package net.ihiroky.niotty.buffer;

/**
 * @author Hiroki Itoh
 */
public interface CodecBufferFactory {

    CodecBuffer newCodecBuffer(int bytes);
    CodecBuffer newCodecBuffer(int bytes, int priority);
}
