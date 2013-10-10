package net.ihiroky.niotty;

import net.ihiroky.niotty.util.Arguments;

import java.util.concurrent.ThreadFactory;

/**
 * @author Hiroki Itoh
 */
public class NameCountThreadFactory implements ThreadFactory {

    private final String threadNamePrefix_;
    private final int priority_;
    private int count_;

    /**
     * Creates a new instance.
     * @param threadNamePrefix a string which prepended to a sequential number
     */
    public NameCountThreadFactory(String threadNamePrefix) {
        Arguments.requireNonNull(threadNamePrefix, "threadNamePrefix");
        threadNamePrefix_ = threadNamePrefix.concat(":");
        priority_ = Thread.NORM_PRIORITY;
    }

    /**
     * Creates a new instance.
     * @param threadNamePrefix a string which prepended to a sequential number
     * @param priority a priority of thread created by this instance
     */
    public NameCountThreadFactory(String threadNamePrefix, int priority) {
        threadNamePrefix_ = Arguments.requireNonNull(threadNamePrefix, "name").concat(":");
        priority_ = Arguments.requireInRange(priority, "priority", Thread.MIN_PRIORITY, Thread.MAX_PRIORITY);
    }

    @Override
    public Thread newThread(Runnable r) {
        Thread thread = new Thread(r, threadNamePrefix_.concat(Integer.toString(count_++)));
        thread.setPriority(priority_);
        return thread;
    }
}
