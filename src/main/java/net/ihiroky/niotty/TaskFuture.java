package net.ihiroky.niotty;

import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

/**
*
*/
public class TaskFuture implements Comparable<TaskFuture> {
    final long expire_;
    final Task task_;
    private volatile int state_ = WAITING;

    private static final AtomicIntegerFieldUpdater<TaskFuture> STATE_UPDATER =
            AtomicIntegerFieldUpdater.newUpdater(TaskFuture.class, "state_");

    private static final int WAITING = 0;
    private static final int READY = 1;
    private static final int DISPATCHED = 2;
    private static final int CANCELLED = 3;

    TaskFuture(long expire, Task task) {
        expire_ = expire;
        task_ = task;

    }

    boolean readyToDispatch() {
        return STATE_UPDATER.compareAndSet(this, WAITING, READY);
    }

    void dispatched() {
        state_ = DISPATCHED;
    }

    public void cancel() {
        STATE_UPDATER.compareAndSet(this, WAITING, CANCELLED);
    }

    public boolean isCancelled() {
        return state_ == CANCELLED;
    }

    public boolean isDispatched() {
        return state_ == DISPATCHED;
    }

    public boolean isDone() {
        return state_ == CANCELLED || state_ == DISPATCHED;
    }

    @Override
    public int compareTo(TaskFuture o) {
        return (expire_ > o.expire_)
                ? 1
                : ((expire_ < o.expire_) ? -1 : 0);
    }

    @Override
    public String toString() {
        return "(expire:" + expire_ + ", task:" + task_ + ", state:" + state_ + ")";
    }
}
