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
 * {@link #poll(boolean)} and {@link #wakeUp()} of this sub class, this class provides
 * the queue only.
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
    private final TaskTimer taskTimer_;
    private volatile Thread thread_;
    private final Set<TaskSelection> selectionSet_;
    private final AtomicInteger weight_ = new AtomicInteger();

    private Logger logger_ = LoggerFactory.getLogger(TaskLoop.class);

    /** The value passed to {@link #poll(boolean)} to indicate that the thread should wait without timeout. */
    public static final long DONE = -1L;

    /** The value passed to {@link #poll(boolean)} to indicate that the thread should not wait. */
    public static final long RETRY_IMMEDIATELY = 0L;

    private static final TimeUnit TIME_UNIT = TimeUnit.MILLISECONDS;
    private static final int INITIAL_TASK_BUFFER_SIZE = 1024;

    /**
     * Creates a new instance.
     * @param taskTimer the timer to execute the tasks.
     * @throws NullPointerException if timer is null.
     */
    protected TaskLoop(TaskTimer taskTimer) {
        Objects.requireNonNull(taskTimer, "taskTimer");
        taskQueue_ = new ConcurrentLinkedQueue<>();
        selectionSet_ = new HashSet<>();
        taskTimer_ = taskTimer;
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
    public TaskTimer.Future offerTask(Task task, long delay, TimeUnit timeUnit) {
        Objects.requireNonNull(task, "task");
        Objects.requireNonNull(timeUnit, "timeUnit");

        return taskTimer_.offer(this, task, delay, timeUnit);
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
                    taskTimer_.offer(this, task, waitTimeMillis, TIME_UNIT);
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

    /**
     * Returns true if the task queue holds tasks.
     * @return true if the task queue holds tasks
     */
    public boolean hasTask() {
        return !taskQueue_.isEmpty() || taskTimer_.hasTask();
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
        try {
            synchronized (this) {
                thread_ = Thread.currentThread();
                taskTimer_.start();
                onOpen();
                notifyAll(); // Counter part: waitUntilStarted()
            }

            while (thread_ != null) {
                try {
                    poll(taskQueue.isEmpty());
                    processTasks(taskQueue, taskBuffer);
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
            taskTimer_.stop();
            thread_ = null;
            taskQueue.clear();
            synchronized (selectionSet_) {
                selectionSet_.clear();
            }
        }
    }

    private void processTasks(Queue<Task> queue, Deque<Task> buffer) throws Exception {
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
                taskTimer_.offer(this, task, retryDelay, TIME_UNIT);
            }
        }
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
     * This method is called before the task execution to process requests if any.
     * The implementation is required to wait properly to avoid busy loop
     * and to process requests managed by the implementation class
     * as necessary.
     *
     * @param  preferToWait true if the implementation should wait until {@link #wakeUp()} is called
     * @throws Exception if some error occurs
     */
    protected abstract void poll(boolean preferToWait) throws Exception;

    /**
     * This method is called when a new task is inserted to the task queue.
     * The implementation is required not to wait in the {@link #poll(boolean)}
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
         * @return {@link net.ihiroky.niotty.TaskLoop DONE} if task is normally finished,
         *   {@link net.ihiroky.niotty.TaskLoop RETRY_IMMEDIATELY} if task is required to execute again immediately,
         *   {@code timeUnit.convert(delay, unitOfDelay)} if task is required to execute again with the specified
         *   delay.
         * @throws Exception if some error occurs
         */
        long execute(TimeUnit timeUnit) throws Exception;
    }
}
