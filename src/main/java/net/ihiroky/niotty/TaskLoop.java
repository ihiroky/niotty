package net.ihiroky.niotty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Provides an loop to process tasks which is queued in a task queue.
 * <p>
 * The task, which implements {@link Task}, is queued by
 * {@link #offerTask(Task)}. It is processed by a dedicated
 * thread in queued (FIFO) order. A queue blocking strategy is determined by
 * {@link #poll(long, TimeUnit)} and {@link #wakeUp()} of this sub class, this class provides
 * the queue only.
 * </p>
 * <p>
 * This class has a timer to process a task with some delay.
 * {@link #offerTask(Task, long, java.util.concurrent.TimeUnit)}
 * is used to register a task to the timer. If a task returns a positive value, the task is
 * registered to the timer implicitly to be processed after the returned value. If returns
 * zero, the task is inserted to the task queue to processed again immediately.
 * </p>
 * <p>
 * This class holds a set of {@link net.ihiroky.niotty.TaskSelection}. The selection shows
 * a object which is associated with this task loop. The sum of the weight of the selections
 * can be used to control the balancing of the association.
 * </p>
 */
public abstract class TaskLoop implements Runnable, Comparable<TaskLoop> {

    private final Queue<Task> taskQueue_;
    private final Queue<TaskFuture> delayQueue_;
    private volatile Thread thread_;
    private final Set<TaskSelection> selectionSet_;
    private final AtomicInteger weight_ = new AtomicInteger();

    private Logger logger_ = LoggerFactory.getLogger(TaskLoop.class);

    /** The value passed to {@link #poll(long, TimeUnit)} to indicate that the thread should wait without timeout. */
    public static final long DONE = -1L;

    /** The value passed to {@link #poll(long, TimeUnit)} to indicate that the thread should not wait. */
    public static final long RETRY_IMMEDIATELY = 0L;

    private static final TimeUnit TIME_UNIT = TimeUnit.NANOSECONDS;
    private static final int INITIAL_TASK_BUFFER_SIZE = 1024;
    private static final int INITIAL_DELAY_QUEUE_SIZE = 1024;

    /**
     * Creates a new instance.
     */
    protected TaskLoop() {
        taskQueue_ = new ConcurrentLinkedQueue<>();
        delayQueue_ = new PriorityQueue<>(INITIAL_DELAY_QUEUE_SIZE);
        selectionSet_ = new HashSet<>();
    }

    /**
     * Inserts a specified task to the task queue.
     * @param task the task to be inserted to the task queue
     * @throws NullPointerException the task is null
     */
    public void offerTask(Task task) {
        taskQueue_.offer(task);
        wakeUp();
    }

    /**
     * Registers a specified task to the timer with specified delay time.
     * @param task the task to be registered to the timer
     * @param delay the delay of task execution
     * @param timeUnit unit of the delay
     * @return a future representing pending completion of the task
     * @throws NullPointerException if task or timeUnit is null
     */
    public TaskFuture offerTask(final Task task, long delay, TimeUnit timeUnit) {
        Objects.requireNonNull(task, "task");
        Objects.requireNonNull(timeUnit, "timeUnit");

        long expire = System.nanoTime() + timeUnit.toNanos(delay);
        final TaskFuture future = new TaskFuture(expire, task);
        taskQueue_.offer(new Task() {
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
     * If a caller is executed in the loop thread, run the task immediately.
     * Otherwise, inserts the task to the task queue.
     * @param task the task
     * @throws NullPointerException if the task is null
     */
    public void executeTask(Task task) {
        if (isInLoopThread()) {
            try {
                long waitTimeMillis = task.execute(TIME_UNIT);
                if (waitTimeMillis > 0) {
                    long expire = System.currentTimeMillis() + waitTimeMillis;
                    delayQueue_.offer(new TaskFuture(expire, task));
                } else if (waitTimeMillis == RETRY_IMMEDIATELY) {
                    taskQueue_.offer(task);
                }
            } catch (Exception e) {
                logger_.warn("[runTask] Unexpected exception.", e);
            }
        } else {
            taskQueue_.offer(task);
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
     * Executes the loop especially on a thread provided by {@link net.ihiroky.niotty.TaskLoopGroup}.
     */
    public void run() {
        Deque<Task> taskBuffer = new ArrayDeque<>(INITIAL_TASK_BUFFER_SIZE);
        Queue<Task> taskQueue = taskQueue_;
        Queue<TaskFuture> delayQueue = delayQueue_;
        try {
            synchronized (this) {
                thread_ = Thread.currentThread();
                onOpen();
                notifyAll(); // Counter part: waitUntilStarted()
            }

            long waitNanos = DONE;
            while (thread_ != null) {
                try {
                    poll(waitNanos, TIME_UNIT);
                    processTasks(taskQueue, taskBuffer, delayQueue);
                    waitNanos = processDelayedTask(delayQueue);
                } catch (InterruptedException ie) {
                    logger_.debug("[run] Interrupted.", ie);
                    break;
                } catch (Exception e) {
                    if (thread_ != null) {
                        logger_.warn("[run] Unexpected exception.", e);
                    }
                }
            }
        } finally {
            onClose();
            taskQueue.clear();
            synchronized (selectionSet_) {
                selectionSet_.clear();
            }
            thread_ = null;
        }
    }

    private void processTasks(Queue<Task> queue, Deque<Task> buffer, Queue<TaskFuture> delayQueue) throws Exception {
        Task task;
        for (;;) {
            task = queue.poll();
            if (task == null) {
                break;
            }
            buffer.offerLast(task);
        }
        for (;;) {
            task = buffer.pollFirst();
            if (task == null) {
                break;
            }
            long retryDelay = task.execute(TIME_UNIT);
            if (retryDelay <= DONE) {
                continue;
            }
            if (retryDelay == RETRY_IMMEDIATELY) {
                queue.offer(task);
            } else { // if (retryDelay > 0) {
                long expire = System.nanoTime() + retryDelay;
                delayQueue.offer(new TaskFuture(expire, task));
            }
        }
    }

    private long processDelayedTask(Queue<TaskFuture> queue) {
        long now = System.nanoTime();
        TaskFuture e;
        for (;;) {
            e = queue.peek();
            if (e == null || e.expire_ > now) {
                break;
            }
            if (!e.readyToDispatch()) {
                queue.poll();
                continue;
            }
            offerTask(e.task_);
            e.dispatched();
            queue.poll();
        }
        while (e != null && e.isCancelled()) {
            queue.poll();
            e = queue.peek();
        }
        return (e != null) ? e.expire_ - now : DONE;
    }

    /**
     * Returns true if the caller is executed on the thread which executes this loop.
     * @return true if the caller is executed on the thread which executes this loop
     */
    protected boolean isInLoopThread() {
        return Thread.currentThread() == thread_;
    }

    /**
     * Returns true if the thread which executes this loop is alive.
     * @return true if the thread which executes this loop is alive
     */
    protected boolean isAlive() {
        Thread t = thread_;
        return (t != null) && t.isAlive();
    }

    void close() {
        Thread t = thread_;
        if (t != null) {
            t.interrupt();
            thread_ = null;
        }
    }

    @Override
    public String toString() {
        return (thread_ != null) ? thread_.getName() : super.toString();
    }

    @Override
    public int compareTo(TaskLoop that) {
        return this.weight_.get() - that.weight_.get();
    }

    /**
     * Returns true if a object specified by the selection is associated with this object.
     * @param selection the object to test
     * @return true if a object specified by the selection is associated with this object
     */
    public boolean contains(TaskSelection selection) {
        synchronized (selectionSet_) {
            return selectionSet_.contains(selection);
        }
    }

    /**
     * Associates a object specified by the selection with this object.
     * @param selection the object to be associated
     * @return sum of the selection's weight associated with this object
     * @throws NullPointerException if selection is null
     */
    public int accept(TaskSelection selection) {
        Objects.requireNonNull(selection, "selection");
        int weight = -1;
        synchronized (selectionSet_) {
            if (!selectionSet_.contains(selection)) {
                weight = addWeight(selection.weight());
                selectionSet_.add(selection);
            }
        }
        return weight;
    }

    /**
     * Dissociate a object specified by the selection from this object.
     * @param selection the object to be dissociate
     * @return sum of the selection's weight associated with this object, exclude the selection
     */
    public int reject(TaskSelection selection) {
        Objects.requireNonNull(selection, "selection");
        int weight = -1;
        synchronized (selectionSet_) {
            if (selectionSet_.contains(selection)) {
                weight = addWeight(-selection.weight());
                selectionSet_.remove(selection);
            }
        }
        return weight;
    }

    /**
     * Returns a view of the selections associated with this object.
     * @return a view of the selections associated with this object
     */
    public Collection<TaskSelection> selectionView() {
        List<TaskSelection> copy;
        synchronized (selectionSet_) {
            copy = new ArrayList<>(selectionSet_);
        }
        return copy;
    }

    private int addWeight(int weight) {
        for (;;) {
            int value = weight_.get();
            int added = value + weight;
            if (added < 0) {
                added = (weight > 0) ? Integer.MAX_VALUE : 0;
            }
            if (weight_.compareAndSet(value, added)) {
                return added;
            }
        }
    }

    /**
     * Returns sum of the weight of selections which is associated with this object.
     * @return sum of the weight of selections
     */
    protected int weight() {
        return weight_.get();
    }

    /**
     * This method is called once when the loop is initialized.
     */
    protected abstract void onOpen();

    /**
     * This method is called once when the loop is ready to terminate.
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
     * This method is called when a new task is inserted to the task queue.
     * The implementation is required to wake up the thread executing
     * {@link #poll(long, java.util.concurrent.TimeUnit)} on waiting timeout.
     */
    protected abstract void wakeUp();

}
