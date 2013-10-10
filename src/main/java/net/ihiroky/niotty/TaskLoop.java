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
 * Provides an loop to process tasks which is queued in a task queue.
 * <p>
 * The task, which implements {@link Task}, is queued by
 * {@link #offer(Task)}. It is processed by a dedicated
 * thread in queued (FIFO) order. A queue blocking strategy is determined by
 * {@link #poll(long, TimeUnit)} and {@link #wakeUp()} of this sub class, this class provides
 * the queue only.
 * </p>
 * <p>
 * This class has a timer to process a task with some delay.
 * {@link #schedule(Task, long, java.util.concurrent.TimeUnit)}
 * is used to register a task to the timer. If a task returns a positive value, the task is
 * registered to the timer implicitly to be processed after the returned value. If returns
 * zero, the task is inserted to the task queue to processed again immediately.
 * </p>
 * <p>
 * This class holds a set of {@link TaskSelection}. The selection shows
 * a object which is associated with this task loop. The number of selections can be used
 * to control the balancing of the association.
 * </p>
 */
public abstract class TaskLoop implements Runnable, Comparable<TaskLoop> {

    private final Queue<Task> taskQueue_;
    private final Queue<TaskFuture> delayQueue_;
    private volatile Thread thread_;
    private final Map<TaskSelection, Integer> selectionCountMap_;

    private Logger logger_ = LoggerFactory.getLogger(TaskLoop.class);

    private static final TimeUnit TIME_UNIT = TimeUnit.NANOSECONDS;
    private static final int INITIAL_TASK_BUFFER_SIZE = 1024;
    private static final int INITIAL_DELAY_QUEUE_SIZE = 1024;

    /**
     * Creates a new instance.
     */
    protected TaskLoop() {
        taskQueue_ = new ConcurrentLinkedQueue<Task>();
        delayQueue_ = new PriorityQueue<TaskFuture>(INITIAL_DELAY_QUEUE_SIZE);
        selectionCountMap_ = new HashMap<TaskSelection, Integer>();
    }

    void close() {
        Thread t = thread_;
        if (t != null) {
            t.interrupt();
            thread_ = null;
        }
    }

    /**
     * Inserts a specified task to the task queue.
     * @param task the task to be inserted to the task queue
     * @throws NullPointerException the task is null
     */
    public void offer(Task task) {
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
    public TaskFuture schedule(final Task task, long delay, TimeUnit timeUnit) {
        Arguments.requireNonNull(task, "task");
        Arguments.requireNonNull(timeUnit, "timeUnit");

        long expire = System.nanoTime() + timeUnit.toNanos(delay);
        final TaskFuture future = new TaskFuture(expire, task);

        if (isInLoopThread()) {
            delayQueue_.offer(future);
            wakeUp();
            return future;
        }

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
    public void execute(Task task) {
        if (isInLoopThread()) {
            try {
                long waitTimeNanos = task.execute(TIME_UNIT);
                if (waitTimeNanos == Long.MAX_VALUE) {
                    return;
                }
                if (waitTimeNanos > 0) {
                    long expire = System.nanoTime() + waitTimeNanos;
                    if (expire < 0) {
                        logger_.warn("[execute] The expire for {} is overflowed. Skip to schedule.", task);
                        return;
                    }
                    delayQueue_.offer(new TaskFuture(expire, task));
                    wakeUp();
                } else {
                    taskQueue_.offer(task);
                    wakeUp();
                }
            } catch (Exception e) {
                logger_.warn("[execute] Unexpected exception.", e);
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
     * Executes the loop especially on a thread provided by {@link TaskLoopGroup}.
     */
    public void run() {
        Deque<Task> taskBuffer = new ArrayDeque<Task>(INITIAL_TASK_BUFFER_SIZE);
        Queue<Task> taskQueue = taskQueue_;
        Queue<TaskFuture> delayQueue = delayQueue_;
        try {
            synchronized (this) {
                thread_ = Thread.currentThread();
                onOpen();
                notifyAll(); // Counter part: waitUntilStarted()
            }

            long delayNanos = Long.MAX_VALUE;
            while (thread_ != null) {
                try {
                    poll(taskQueue.isEmpty() ? delayNanos : Task.RETRY_IMMEDIATELY, TIME_UNIT);
                    processTasks(taskQueue, taskBuffer, delayQueue);
                    delayNanos = processDelayedTask(taskQueue, delayQueue);
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
            synchronized (selectionCountMap_) {
                selectionCountMap_.clear();
            }
            thread_ = null;
        }
    }

    private void processTasks(
            Queue<Task> taskQueue, Deque<Task> buffer, Queue<TaskFuture> delayQueue) throws Exception {
        Task task;
        for (;;) {
            task = taskQueue.poll();
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
            if (retryDelay == Long.MAX_VALUE) {
                continue;
            }
            if (retryDelay > 0) {
                long expire = System.nanoTime() + retryDelay;
                if (expire < 0) {
                    logger_.warn("[processTask] The expire for {} is overflowed. Skip to schedule.", task);
                    continue;
                }
                delayQueue.offer(new TaskFuture(expire, task));
            } else {
                taskQueue.offer(task);
            }
        }
    }

    private long processDelayedTask(Queue<Task> taskQueue, Queue<TaskFuture> delayQueue) throws Exception {
        long now = System.nanoTime();
        TaskFuture f;
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
                long waitTimeNanos = f.task_.execute(TIME_UNIT);
                if (waitTimeNanos == Long.MAX_VALUE) {
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
                    logger_.warn("[processDelayedTask] The expire for {} is overflowed. Skip to schedule.", f.task_);
                } else {
                    taskQueue.offer(f.task_);
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
     * Returns true if the caller is executed on the thread which executes this loop.
     * @return true if the caller is executed on the thread which executes this loop
     */
    public boolean isInLoopThread() {
        return Thread.currentThread() == thread_;
    }

    /**
     * Returns true if the thread which executes this loop is alive.
     * @return true if the thread which executes this loop is alive
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
    public int compareTo(TaskLoop that) {
        return selectionCount() - that.selectionCount();
    }

    int selectionCount() {
        synchronized (selectionCountMap_) {
            return selectionCountMap_.size();
        }
    }

    int duplicationCountFor(TaskSelection selection) {
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
    boolean countUpDuplication(TaskSelection selection) {
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
    public int accept(TaskSelection selection) {
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
    public int reject(TaskSelection selection) {
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
