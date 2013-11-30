package net.ihiroky.niotty.nio;

/**
 * A queue to holds messages to be written to a channel.
  */
public interface WriteQueue {

    /**
     * Returns the number of elements in this queue.
     * @return the number of elements in this queue
     */
    int size();

    /**
     * Returns true if this queue contains no elements.
     * @return true if this queue contains no elements
     */
    boolean isEmpty();

    /**
     * Clears elements in this queue.
     */
    void clear();

}
