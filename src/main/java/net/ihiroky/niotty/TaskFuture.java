package net.ihiroky.niotty;

import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

/**
 * A future represents the result of a delayed task scheduled by
 * {@link TaskLoop#schedule(Task, long, java.util.concurrent.TimeUnit)}.
 *
 * This class has a state. If the task is cancelled, then {@link #isCancelled()}
 * returns true. If dispatched (the task was executed or retried immediately), then
 * {@link #isDispatched()} returns true. If cancelled or dispatched, then
 * {@link #isDone()} returns true.
 */
public class TaskFuture implements Comparable<TaskFuture> {
    private long expire_;
    final Task task_;
    private volatile State state_;

    private static final AtomicReferenceFieldUpdater<TaskFuture, State> STATE_UPDATER =
            AtomicReferenceFieldUpdater.newUpdater(TaskFuture.class, State.class, "state_");

    private enum State {
        WAITING,
        READY,
        DISPATCHED,
        CANCELLED,
        ;
    }

    TaskFuture(long expire, Task task) {
        expire_ = expire;
        task_ = task;
        state_ = State.WAITING;
    }

    long expire() {
        return expire_;
    }

    void setExpire(long expire) {
        expire_ = expire;
        state_ = State.WAITING;
    }

    boolean readyToDispatch() {
        return STATE_UPDATER.compareAndSet(this, State.WAITING, State.READY);
    }

    void dispatched() {
        state_ = State.DISPATCHED;
    }

    /**
     * Cancels the task if not dispatched.
     */
    public void cancel() {
        STATE_UPDATER.compareAndSet(this, State.WAITING, State.CANCELLED);
    }

    /**
     * Returns true if the task is cancelled.
     * @return true if the task is cancelled
     */
    public boolean isCancelled() {
        return state_ == State.CANCELLED;
    }

    /**
     * Returns true if the task is dispatched.
     * @return true if the task is dispatched
     */
    public boolean isDispatched() {
        return state_ == State.DISPATCHED;
    }

    /**
     * Returns true if the task is cancelled or dispatched.
     * @return true if the task is cancelled or dispatched
     */
    public boolean isDone() {
        return state_ == State.CANCELLED || state_ == State.DISPATCHED;
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
