package net.ihiroky.niotty;

import net.ihiroky.niotty.util.Arguments;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

/**
 * Provides an dispatcher to process events which is queued in a event queue.
 * <p>
 * The event, which implements {@link Event}, is queued by
 * {@link #offer(Event)}. It is processed by a dedicated
 * thread in queued (FIFO) order. A queue blocking strategy is determined by
 * {@link #poll(long, TimeUnit)} and {@link #wakeUp()} of this sub class, this class provides
 * the queue only.
 * </p>
 * <p>
 * This class has a timer to process a event with some delay.
 * {@link #schedule(Event, long, java.util.concurrent.TimeUnit)}
 * is used to register a event to the timer. If a event returns a positive value, the event is
 * registered to the timer implicitly to be processed after the returned value. If returns
 * zero, the event is inserted to the event queue to processed again immediately.
 * </p>
 * <p>
 * This class holds a set of {@link EventDispatcherSelection}. The selection shows
 * a object which is associated with this event dispatcher. The number of selections can be used
 * to control the balancing of the association.
 * </p>
 */
public abstract class EventDispatcher implements Runnable, Comparable<EventDispatcher> {

    private final Queue<Event> eventQueue_;
    private final Queue<EventFuture> delayQueue_;
    private volatile Thread thread_;
    private final Map<EventDispatcherSelection, Integer> selectionCountMap_;

    private Logger logger_ = LoggerFactory.getLogger(EventDispatcher.class);

    private static final TimeUnit TIME_UNIT = TimeUnit.NANOSECONDS;
    private static final int INITIAL_EVENT_BUFFER_SIZE = 1024;
    private static final int INITIAL_DELAY_QUEUE_SIZE = 1024;

    /**
     * Creates a new instance.
     */
    protected EventDispatcher() {
        eventQueue_ = new ConcurrentLinkedQueue<Event>();
        delayQueue_ = new PriorityQueue<EventFuture>(INITIAL_DELAY_QUEUE_SIZE);
        selectionCountMap_ = new HashMap<EventDispatcherSelection, Integer>();
    }

    void close() {
        Thread t = thread_;
        if (t != null) {
            t.interrupt();
            thread_ = null;
        }
    }

    protected Thread thread() {
        return thread_;
    }

    /**
     * Inserts a specified event to the event queue.
     * @param event the event to be inserted to the event queue
     * @throws NullPointerException the event is null
     */
    public void offer(Event event) {
        eventQueue_.offer(event);
        wakeUp();
    }

    /**
     * Registers a specified event to the timer with specified delay time.
     * @param event the event to be registered to the timer
     * @param delay the delay of event execution
     * @param timeUnit unit of the delay
     * @return a future representing pending completion of the event
     * @throws NullPointerException if event or timeUnit is null
     */
    public EventFuture schedule(final Event event, long delay, TimeUnit timeUnit) {
        Arguments.requireNonNull(event, "event");
        Arguments.requireNonNull(timeUnit, "timeUnit");

        long expire = System.nanoTime() + timeUnit.toNanos(delay);
        final EventFuture future = new EventFuture(expire, event);

        if (isInDispatcherThread()) {
            delayQueue_.offer(future);
            wakeUp();
            return future;
        }

        eventQueue_.offer(new Event() {
            @Override
            public long execute(TimeUnit timeUnit) throws Exception {
                delayQueue_.offer(future);
                return DONE;
            }
        });
        wakeUp();
        return future;
    }

    /**
     * If a caller is executed in the dispatcher thread, run the event immediately.
     * Otherwise, inserts the event to the event queue.
     * @param event the event
     * @throws NullPointerException if the event is null
     */
    public void execute(Event event) {
        if (isInDispatcherThread()) {
            try {
                long waitTimeNanos = event.execute(TIME_UNIT);
                if (waitTimeNanos == Event.DONE) {
                    return;
                }
                if (waitTimeNanos > 0) {
                    long expire = System.nanoTime() + waitTimeNanos;
                    if (expire < 0) {
                        logger_.warn("[execute] The expire for {} is overflowed. Skip to schedule.", event);
                        return;
                    }
                    delayQueue_.offer(new EventFuture(expire, event));
                    wakeUp();
                } else {
                    eventQueue_.offer(event);
                    wakeUp();
                }
            } catch (Exception e) {
                logger_.warn("[execute] Unexpected exception.", e);
            }
        } else {
            eventQueue_.offer(event);
            wakeUp();
        }
    }

    void waitUntilStarted() throws InterruptedException {
        synchronized (this) {
            while (thread_ == null) {
                wait();
            }
        }
    }

    /**
     * Executes the dispatcher on a thread provided by {@link EventDispatcherGroup}.
     */
    public void run() {
        Deque<Event> eventBuffer = new ArrayDeque<Event>(INITIAL_EVENT_BUFFER_SIZE);
        Queue<Event> eventQueue = eventQueue_;
        Queue<EventFuture> delayQueue = delayQueue_;
        try {
            synchronized (this) {
                thread_ = Thread.currentThread();
                onOpen();
                notifyAll(); // Counter part: waitUntilStarted()
            }

            long delayNanos = Long.MAX_VALUE;
            while (thread_ != null) {
                try {
                    poll(eventQueue.isEmpty() ? delayNanos : Event.RETRY_IMMEDIATELY, TIME_UNIT);
                    processEvents(eventQueue, eventBuffer, delayQueue);
                    delayNanos = processDelayedEvent(eventQueue, delayQueue);
                } catch (InterruptedException ie) {
                    logger_.debug("[run] Interrupted.", ie);
                    break;
                } catch (Exception e) {
                    if (thread_ != null) {
                        logger_.warn("[run] Unexpected exception.", e);
                    }
                }
                while (!eventBuffer.isEmpty()) {
                    eventQueue.offer(eventBuffer.pollFirst());
                }
            }
        } finally {
            onClose();
            eventQueue.clear();
            synchronized (selectionCountMap_) {
                selectionCountMap_.clear();
            }
            thread_ = null;
        }
    }

    private void processEvents(
            Queue<Event> eventQueue, Deque<Event> buffer, Queue<EventFuture> delayQueue) throws Exception {
        Event event;
        for (;;) {
            event = eventQueue.poll();
            if (event == null) {
                break;
            }
            long retryDelay = event.execute(TIME_UNIT);
            if (retryDelay == Event.DONE) {
                continue;
            }
            if (retryDelay > 0) {
                long expire = System.nanoTime() + retryDelay;
                if (expire < 0) {
                    logger_.warn("[processEvent] The expire for {} is overflowed. Skip to schedule.", event);
                    continue;
                }
                delayQueue.offer(new EventFuture(expire, event));
            } else {
                buffer.offerLast(event);
            }
        }
    }

    private long processDelayedEvent(Queue<Event> eventQueue, Queue<EventFuture> delayQueue) throws Exception {
        long now = System.nanoTime();
        EventFuture f;
        for (;;) {
            f = delayQueue.peek();
            if (f == null || f.expire() > now) {
                break;
            }
            if (!f.readyToDispatch()) {
                delayQueue.poll();
                continue;
            }

            try {
                delayQueue.poll();
                long waitTimeNanos = f.event_.execute(TIME_UNIT);
                if (waitTimeNanos == Event.DONE) {
                    f.dispatched();
                    continue;
                }
                if (waitTimeNanos > 0) {
                    long expire = now + waitTimeNanos;
                    if (expire > 0) {
                        f.setExpire(expire);
                        delayQueue.offer(f);
                        continue;
                    }
                    logger_.warn("[processDelayedEvent] The expire for {} is overflowed. Skip to schedule.", f.event_);
                } else {
                    eventQueue.offer(f.event_);
                }
                f.dispatched();
            } catch (Exception ex) {
                logger_.warn("[execute] Unexpected exception.", ex);
            }
        }
        while (f != null && f.isCancelled()) {
            delayQueue.poll();
            f = delayQueue.peek();
        }
        return (f != null) ? f.expire() - now : Long.MAX_VALUE;
    }

    /**
     * Returns true if the caller is executed on the thread which executes this dispatcher.
     * @return true if the caller is executed on the thread which executes this dispatcher
     */
    public boolean isInDispatcherThread() {
        return Thread.currentThread() == thread_;
    }

    /**
     * Returns true if the thread which executes this dispatcher is alive.
     * @return true if the thread which executes this dispatcher is alive
     */
    public boolean isAlive() {
        Thread t = thread_;
        return (t != null) && t.isAlive();
    }

    @Override
    public String toString() {
        return (thread_ != null) ? thread_.toString() : super.toString();
    }

    @Override
    public int compareTo(EventDispatcher that) {
        return selectionCount() - that.selectionCount();
    }

    int selectionCount() {
        synchronized (selectionCountMap_) {
            return selectionCountMap_.size();
        }
    }

    int duplicationCountFor(EventDispatcherSelection selection) {
        Integer count;
        synchronized (selectionCountMap_) {
            count = selectionCountMap_.get(selection);
        }
        return (count != null) ? count : 0;
    }

    /**
     * Counts the duplication counter by one if a object specified by the selection is associated with this object.
     * @param selection the object to test
     * @return true if a object specified by the selection is associated with this object
     */
    boolean countUpDuplication(EventDispatcherSelection selection) {
        Integer count;
        synchronized (selectionCountMap_) {
            count = selectionCountMap_.get(selection);
            if (count != null) {
                selectionCountMap_.put(selection, count + 1);
            }
        }
        return count != null;
    }

    /**
     * Associates a object specified by the selection with this object.
     * @param selection the object to be associated
     * @return the number of the selections associated with this object
     * @throws NullPointerException if selection is null
     */
    public int accept(EventDispatcherSelection selection) {
        Arguments.requireNonNull(selection, "selection");
        int size;
        synchronized (selectionCountMap_) {
            Integer count = selectionCountMap_.get(selection);
            if (count == null) {
                count = 0;
            }
            selectionCountMap_.put(selection, count + 1);
            size = selectionCountMap_.size();
        }
        return size;
    }

    /**
     * Dissociate a object specified by the selection from this object.
     * @param selection the object to be dissociate
     * @return the number of the selections weight associated with this object, exclude the selection
     */
    public int reject(EventDispatcherSelection selection) {
        Arguments.requireNonNull(selection, "selection");
        int size;
        synchronized (selectionCountMap_) {
            Integer count = selectionCountMap_.get(selection);
            if (count != null) {
                count = count - 1;
                if (count != 0) {
                    selectionCountMap_.put(selection, count);
                } else {
                    selectionCountMap_.remove(selection);
                }
            }
            size = selectionCountMap_.size();
        }
        return size;
    }

    /**
     * This method is called once when the dispatcher is initialized.
     */
    protected abstract void onOpen();

    /**
     * This method is called once when the dispatcher is ready to terminate.
     */
    protected abstract void onClose();

    /**
     * Executes any procedure.
     * This method returns when the procedure was executed, the method {@link #wakeUp()} is invoked,
     * the current thread is interrupted, or the given timeout period expires, whichever comes first.
     *
     * @param timeout a time to block for up to timeout, more or less,
     *                while waiting for a channel to become ready; if zero, block indefinitely;
     *                if negative, returns immediately
     * @param timeUnit the unit of the timeout
     * @throws Exception if some error occurs
     */
    protected abstract void poll(long timeout, TimeUnit timeUnit) throws Exception;

    /**
     * This method is called when a new event is inserted to the event queue.
     * The implementation is required to wake up the thread executing
     * {@link #poll(long, java.util.concurrent.TimeUnit)} on waiting timeout.
     */
    protected abstract void wakeUp();

}
