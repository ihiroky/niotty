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
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created on 13/01/10, 17:56
 *
 * @author Hiroki Itoh
 */
public abstract class TaskLoop implements Runnable, Comparable<TaskLoop> {

    private final Queue<Task> taskQueue_;
    private volatile Thread thread_;
    private final Set<TaskSelection> selectionSet_;
    private final AtomicInteger weight_ = new AtomicInteger();

    private Logger logger_ = LoggerFactory.getLogger(TaskLoop.class);

    /** The value passed to {@link #process(int)} to indicate that the thread should wait without timeout. */
    public static final int WAIT_NO_LIMIT = -1;

    /** The value passed to {@link #process(int)} to indicate that the thread should not wait. */
    public static final int RETRY_IMMEDIATELY = 0;

    protected TaskLoop() {
        taskQueue_ = new ConcurrentLinkedQueue<>();
        selectionSet_ = new HashSet<>();
    }

    public void offerTask(Task task) {
        taskQueue_.offer(task);
        wakeUp();
    }

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

    public void run() {
        synchronized (this) {
            thread_ = Thread.currentThread();
            notifyAll();
        }

        Queue<Task> taskBuffer = new LinkedList<>();
        int waitTimeMillis = RETRY_IMMEDIATELY;
        Queue<Task> taskQueue = taskQueue_;
        try {
            onOpen();
            while (thread_ != null) {
                try {
                    process(waitTimeMillis);
                    waitTimeMillis = processTasks(taskQueue, taskBuffer);
                } catch (InterruptedException ie) {
                    logger_.debug("[run] Interrupted.", ie);
                    break;
                } catch (Exception e) {
                    if (thread_ != null) {
                        logger_.warn("[run] process failed.", e);
                        waitTimeMillis = RETRY_IMMEDIATELY;
                    }
                }
            }
        } finally {
            onClose();
            thread_ = null;
            taskQueue.clear();
            synchronized (selectionSet_) {
                selectionSet_.clear();
            }
        }
    }

    private int processTasks(Queue<Task> queue, Queue<Task> buffer) throws Exception {
        int minWaitTimeMillis = Integer.MAX_VALUE;
        for (Task task;;) {
            task = queue.poll();
            if (task == null) {
                break;
            }
            int waitTimeMillis = task.execute();
            if (waitTimeMillis >= 0) {
                buffer.offer(task);
                if (minWaitTimeMillis > waitTimeMillis) {
                    minWaitTimeMillis = waitTimeMillis;
                }
            }
        }
        if (minWaitTimeMillis == Integer.MAX_VALUE) {
            return WAIT_NO_LIMIT;
        } else {
            queue.addAll(buffer);
            buffer.clear();
            return minWaitTimeMillis;
        }
    }

    protected boolean isInLoopThread() {
        return Thread.currentThread() == thread_;
    }

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

    public boolean contains(TaskSelection selection) {
        synchronized (selectionSet_) {
            return selectionSet_.contains(selection);
        }
    }

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

    protected int weight() {
        return weight_.get();
    }

    protected abstract void onOpen();
    protected abstract void onClose();
    protected abstract void process(int waitTimeMillis) throws Exception;
    protected abstract void wakeUp();

    public interface Task {
        int execute() throws Exception;
    }
}
