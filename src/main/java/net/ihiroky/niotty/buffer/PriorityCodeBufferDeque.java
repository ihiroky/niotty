package net.ihiroky.niotty.buffer;

/**
 * @author Hiroki Itoh
 */
public class PriorityCodeBufferDeque extends CodecBufferDeque {

    private final int priority_;

    public PriorityCodeBufferDeque(int priority) {
        priority_ = priority;
    }

    @Override
    public int priority() {
        return priority_;
    }
}
