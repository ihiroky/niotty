package net.ihiroky.niotty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Provides an loop to process tasks which is queued in a task queue.
 * <p>
 * The task, which implements {@link net.ihiroky.niotty.TaskLoop.Task}, is queued by
 * {@link #offerTask(net.ihiroky.niotty.TaskLoop.Task)}. It is processed by a dedicated
 * thread in queued (FIFO) order. A queue blocking strategy is determined by
 * {@link #process(long, java.util.concurrent.TimeUnit)} and {@link #wakeUp()} of
 * this sub class, this class provides the queue only.
 * </p>
 * <p>
 * This class has a timer to process a task with some delay.
 * {@link #offerTask(net.ihiroky.niotty.TaskLoop.Task, long, java.util.concurrent.TimeUnit)}
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
    private final TaskTimer timer_;
    private volatile Thread thread_;
    private final Set<TaskSelection> selectionSet_;
    private final AtomicInteger weight_ = new AtomicInteger();

    private Logger logger_ = LoggerFactory.getLogger(TaskLoop.class);

    /** The value passed to {@link #process(long, TimeUnit)} to indicate that the thread should wait without timeout. */
    public static final long WAIT_NO_LIMIT = -1L;

    /** The value passed to {@link #process(long, TimeUnit)} to indicate that the thread should not wait. */
    public static final long RETRY_IMMEDIATELY = 0L;

    private static final TimeUnit TIME_UNIT = TimeUnit.MILLISECONDS;

    /**
     * Creates a new instance.
     */
    protected TaskLoop() {
        taskQueue_ = new ConcurrentLinkedQueue<>();
        selectionSet_ = new HashSet<>();
        timer_ = new DefaultTaskTimer(this);
    }

    /**
     * Creates a new instance.
     * @param timer the timer to execute the tasks.
     * @throws NullPointerException if timer is null.
     */
    protected TaskLoop(TaskTimer timer) {
        Objects.requireNonNull(timer, "timer");
        taskQueue_ = new ConcurrentLinkedQueue<>();
        selectionSet_ = new HashSet<>();
        timer_ = timer;
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
     */
    public void offerTask(Task task, long delay, TimeUnit timeUnit) {
        Objects.requireNonNull(task, "task");
        Objects.requireNonNull(timeUnit, "timeUnit");

        timer_.offer(this, task, delay, timeUnit);
    }

    /**
     * Returns true if the task queue holds no task.
     * @return  true if the task queue holds no task
     */
    public boolean hasNoTask() {
        return taskQueue_.isEmpty();
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
        Queue<Task> taskBuffer = new LinkedList<>();
        Queue<Task> taskQueue = taskQueue_;
        long waitTimeMillis = RETRY_IMMEDIATELY;
        try {
            synchronized (this) {
                thread_ = Thread.currentThread();
                timer_.start();
                onOpen();
                notifyAll(); // Counter part: waitUntilStarted()
            }

            while (thread_ != null) {
                try {
                    process(waitTimeMillis, TIME_UNIT);
                    waitTimeMillis = processTasks(taskQueue, taskBuffer);
                } catch (InterruptedException ie) {
                    logger_.debug("[run] Interrupted.", ie);
                    break;
                } catch (Exception e) {
                    if (thread_ != null) {
                        logger_.warn("[run] Unexpected exception.", e);
                        waitTimeMillis = RETRY_IMMEDIATELY;
                    }
                }
            }
        } finally {
            onClose();
            timer_.stop();
            thread_ = null;
            taskQueue.clear();
            synchronized (selectionSet_) {
                selectionSet_.clear();
            }
        }
    }

    private long processTasks(Queue<Task> queue, Queue<Task> buffer) throws Exception {
        for (Task task;;) {
            task = queue.poll();
            if (task == null) {
                break;
            }

            long waitTimeMillis = task.execute(TIME_UNIT);
            if (waitTimeMillis > 0) {
                timer_.offer(this, task, waitTimeMillis, TIME_UNIT);
            } else if (waitTimeMillis == RETRY_IMMEDIATELY) {
                buffer.offer(task);
            }
        }
        if (buffer.isEmpty()) {
            return timer_.flush(TIME_UNIT);
        }
        queue.addAll(buffer); // These tasks is required to be executed immediately.
        timer_.flush(TIME_UNIT);
        return RETRY_IMMEDIATELY;
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
     * This method is called before the task processing.
     * The implementation is required to wait properly to avoid busy loop
     * and to execute some process managed by the implementation class
     * as necessary.
     *
     * @param timeout the time to be expected to wait
     * @param timeUnit unit of the time
     * @throws Exception if some error occurs
     */
    protected abstract void process(long timeout, TimeUnit timeUnit) throws Exception;

    /**
     * This method is called when a new task is inserted to the task queue.
     * The implementation is required not to wait in the {@link #process(long, java.util.concurrent.TimeUnit)}
     * after this method is called.
     */
    protected abstract void wakeUp();

    /**
     * The task executed by the {@link net.ihiroky.niotty.TaskLoop}.
     */
    public interface Task {
        /**
         * Executes the task procedure.
         * @param timeUnit unit of time returned by this method
         * @return {@link net.ihiroky.niotty.TaskLoop WAIT_NO_LIMIT} if task is normally finished,
         *   {@link net.ihiroky.niotty.TaskLoop RETRY_IMMEDIATELY} if task is required to execute again immediately,
         *   {@code timeUnit.convert(delay, unitOfDelay)} if task is required to execute again with the specified
         *   delay.
         * @throws Exception if some error occurs
         */
        long execute(TimeUnit timeUnit) throws Exception;
    }

    /**
     * Default task timer, which handles wait time using {@link #process(long, java.util.concurrent.TimeUnit)}.
     */
    private static class DefaultTaskTimer implements TaskTimer {

        TaskLoop taskLoop_;
        Queue<Task> buffer_;
        long minWaitTimeMillis_;

        DefaultTaskTimer(TaskLoop taskLoop) {
            taskLoop_ = taskLoop;
            buffer_ = new LinkedList<>();
            minWaitTimeMillis_ = Long.MAX_VALUE;
        }

        @Override
        public void start() {
        }

        @Override
        public void stop() {
        }

        @Override
        public void offer(TaskLoop taskLoop, Task task, long delay, TimeUnit timeUnit) {
            buffer_.offer(task);
            long waitTimeMillis = timeUnit.toMillis(delay);
            if (minWaitTimeMillis_ > waitTimeMillis) {
                minWaitTimeMillis_ = waitTimeMillis;
            }
        }

        @Override
        public long flush(TimeUnit timeUnit) {
            for (Task task : buffer_) {
                taskLoop_.taskQueue_.offer(task);
            }
            long waitTimeMillis = (minWaitTimeMillis_ != Long.MAX_VALUE) ? minWaitTimeMillis_ : WAIT_NO_LIMIT;
            minWaitTimeMillis_ = Long.MAX_VALUE;
            buffer_.clear();
            return timeUnit.convert(waitTimeMillis, TimeUnit.MILLISECONDS);
        }
    }
}
