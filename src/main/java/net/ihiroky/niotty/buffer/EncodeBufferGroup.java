package net.ihiroky.niotty.buffer;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

/**
 * @author Hiroki Itoh
 */
public class EncodeBufferGroup implements Iterable<EncodeBuffer> {

    private Deque<EncodeBuffer> group = new ArrayDeque<>();

    public void addLast(EncodeBuffer encodeBuffer) {
        group.addLast(encodeBuffer);
    }

    public void addFirst(EncodeBuffer encodeBuffer) {
        group.addFirst(encodeBuffer);
    }

    public EncodeBuffer pollFirst() {
        return group.pollFirst();
    }

    public EncodeBuffer pollLast() {
        return group.pollLast();
    }

    @Override
    public Iterator<EncodeBuffer> iterator() {
        return group.iterator();
    }
}
