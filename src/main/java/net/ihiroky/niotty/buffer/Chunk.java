package net.ihiroky.niotty.buffer;

/**
 * A piece of data which content type is E.
 * {@code Chunk}'s life cycle is managed by reference counting. The initial count is -1, pre-initialize state.
 * The count gets 1 when {@link #initialize()} is called. The method {@link #retain()} increments it
 * and {@link #release()} decrements it. This chunk gets an unusable state if it gets from 1 to 0
 * when {@link #release()} is called. To reuse the chunk after released, call {@link #ready()} to change
 * pre-initialize state, this method should be called in a object who manages {@code Chunk} lifecycle like
 * {@link ChunkPool} and users need not to call it.
 * The {@link #initialize()}, {@link #retain()} and {@link #release()} may throw {@code IllegalStateException}
 * if the count is not invalid. The valid count flow is shown below.
 *<pre>
 * |method| - the method invocation.
 * number   - the value of reference count
 *
 *                                +---------|release|
 *                                v             |
 *  initial -> -1 -|initialize|-> 1 -|retain|-> 2 -|retain|-> 3 - ...
 *             ^                  |             ^             |
 *             +-|ready|- 0 <-|release|         +---------|release|
 *</pre>
 * @param <E> type of the content
 * @author Hiroki Itoh
 */
public interface Chunk<E> {

    /**
     * Initializes this chunk status and get its content.
     * The reference count gets 1.
     * This method must be called once when this instance is used at the beginning.
     *
     * @return the content in this chunk
     */
    E initialize();

    /**
     * Gets the content in this chunk and retains this chunk to release it with one more {@link #release()} call.
     * The reference count is incremented when this method is called.
     *
     * @return the content in this chunk
     * @throws IllegalStateException if this chunk is unusable or not initialized (the reference count is 0).
     */
    E retain();

    /**
     * Releases this chunk and make this chunk the unusable state if the reference count gets from 1 to 0.
     * The reference count is decremented when this method is called. And then the reference count is checked
     * if it gets 0.
     *
     * @return the reference count
     */
    int release();

    /**
     * Returns the size of the content in this chunk.
     * @return the size of the content in this chunk.
     */
    int size();

    /**
     * Releases this chunk and allocate a new chunk from a manager which allocates this chunk.
     * The new chunk is allocated, and then {@link #release()} of this chunk is called.
     * @param bytes a size of the new chunk
     * @return the new chunk
     */
    Chunk<E> reallocate(int bytes);

    /**
     * Returns the reference count.
     * @return the reference count
     */
    int referenceCount();

    /**
     * Change this chunk state to pre-initialized state.
     * This method is not needed to be call by users.
     */
    void ready();

    /**
     * Returns a {@code ChunkManager} which manages this chunk lifecycle.
     * @return a {@code ChunkManager} which manages this chunk lifecycle
     */
    ChunkManager<E> manager();
}
