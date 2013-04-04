package net.ihiroky.niotty.buffer;

/**
 * @author Hiroki Itoh
 */
public class PriorityArrayCodecBuffer extends ArrayCodecBuffer {

    private final int priority_;

    PriorityArrayCodecBuffer(int priority) {
        super();
        priority_ = priority;
    }

    PriorityArrayCodecBuffer(int initialCapacity, int priority) {
        super(initialCapacity);
        priority_ = priority;
    }

    PriorityArrayCodecBuffer(byte[] buffer, int offset, int length, int priority) {
        super(buffer, offset, length);
        priority_ = priority;
    }

    @Override
    public int priority() {
        return priority_;
    }
}
