package net.ihiroky.niotty.buffer;

/**
*  TODO introduce reference count in chunk and implements duplicate() for CodecBuffer#slice()?
* @author Hiroki Itoh
*/
public interface Chunk<E> {

    E initialize();
    E retain();
    int release();
    int size();
    Chunk<E> reallocate(int bytes);
    int retainCount();
}
