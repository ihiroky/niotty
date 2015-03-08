package net.ihiroky.niotty;

import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

/**
 * A future represents the result of a delayed event scheduled by
 * {@link EventDispatcher#schedule(Event, long, java.util.concurrent.TimeUnit)}.
 *
 * This class has a state. If the event is cancelled, then {@link #isCancelled()}
 * returns true. If dispatched (the event was executed or retried immediately), then
 * {@link #isDispatched()} returns true. If cancelled or dispatched, then
 * {@link #isDone()} returns true.
 */
public class EventFuture implements Event, Comparable<EventFuture> {
    private long expire_;
    final Event event_;
    private volatile State state_;

    private static final AtomicReferenceFieldUpdater<EventFuture, State> STATE_UPDATER =
            AtomicReferenceFieldUpdater.newUpdater(EventFuture.class, State.class, "state_");

    /**
     * {@inheritDoc}
     *
     * It is assumed that this method is called by the normal event queue, not delayed queue.
     */
    @Override
    public long execute() throws Exception {
        long retryDelay = event_.execute();
        if (retryDelay == DONE) {
            dispatched();
        }
        return retryDelay;
    }

    private enum State {
        WAITING,
        READY,
        DISPATCHED,
        CANCELLED,
        ;
    }

    public EventFuture(long expire, Event event) {
        expire_ = expire;
        event_ = event;
        state_ = State.WAITING;

    }

    long expire() {
        return expire_;
    }

    EventFuture setExpire(long expire) {
        expire_ = expire;
        state_ = State.WAITING;
        return this;
    }

    boolean readyToDispatch() {
        return STATE_UPDATER.compareAndSet(this, State.WAITING, State.READY);
    }

    void dispatched() {
        state_ = State.DISPATCHED;
    }

    /**
     * Cancels the event if not dispatched.
     */
    public void cancel() {
        STATE_UPDATER.compareAndSet(this, State.WAITING, State.CANCELLED);
    }

    /**
     * Returns true if the event is cancelled.
     * @return true if the event is cancelled
     */
    public boolean isCancelled() {
        return state_ == State.CANCELLED;
    }

    /**
     * Returns true if the event is dispatched.
     * @return true if the event is dispatched
     */
    public boolean isDispatched() {
        return state_ == State.DISPATCHED;
    }

    /**
     * Returns true if the event is cancelled or dispatched.
     * @return true if the event is cancelled or dispatched
     */
    public boolean isDone() {
        return state_ == State.CANCELLED || state_ == State.DISPATCHED;
    }

    @Override
    public int compareTo(EventFuture o) {
        return (expire_ > o.expire_)
                ? 1
                : ((expire_ < o.expire_) ? -1 : 0);
    }

    @Override
    public String toString() {
        return "(expire:" + expire_ + ", event:" + event_ + ", state:" + state_ + ")";
    }
}
