package net.ihiroky.niotty;

import net.ihiroky.niotty.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Provides a thread pool to execute {@link TaskLoop} and manages the task loop lifecycle.
 *
 * @param <L> the actual type of the TaskLoop
 * @author Hiroki Itoh
 */
public abstract class TaskLoopGroup<L extends TaskLoop> {

    private final Collection<L> taskLoops_;
    private ThreadPoolExecutor executor_;
    private Logger logger_ = LoggerFactory.getLogger(TaskLoopGroup.class);

    /**
     * Constructs a new instance.
     */
    protected TaskLoopGroup() {
        taskLoops_ = new HashSet<L>();
    }

    /**
     * Creates the thread pool internally if not created.
     * @param threadFactory a thread factory
     * @param workers the number of threads held in the thread pool
     */
    public void open(ThreadFactory threadFactory, int workers) {
        if (workers <= 0) {
            throw new IllegalArgumentException("The workers must be positive.");
        }

        synchronized (taskLoops_) {
            if (executor_ == null) {
                ThreadPoolExecutor executor = new ThreadPoolExecutor(
                        workers, workers, 1L, TimeUnit.MINUTES, new SynchronousQueue<Runnable>(), threadFactory);
                Collection<L> taskLoops = new ArrayList<L>(workers);
                for (int i = 0; i < workers; i++) {
                    L taskLoop = newTaskLoop();
                    logger_.debug("[open] New task loop: {}.", taskLoop);
                    executor.execute(taskLoop);
                    taskLoops.add(taskLoop);
                }
                try {
                    for (L taskLoop : taskLoops) {
                        taskLoop.waitUntilStarted();
                    }
                    executor_ = executor;
                    taskLoops_.addAll(taskLoops);
                } catch (InterruptedException ie) {
                    logger_.debug("Interrupted.", ie);
                }
            }
        }
    }

    /**
     * Stops the task loops and releases the thread pool.
     */
    public void close() {
        synchronized (taskLoops_) {
            for (L taskLoop : taskLoops_) {
                taskLoop.close();
            }
            taskLoops_.clear();
            ExecutorService executor = executor_;
            if (executor != null) {
                executor.shutdownNow();
                executor_ = null;
            }
        }
    }

    /**
     * Returns true if the thread pool is available.
     * @return true if the thread pool is available
     */
    public boolean isOpen() {
        synchronized (taskLoops_) {
            return (executor_ != null) && !executor_.isShutdown();
        }
    }

    /**
     * Offers a task for each task loop.
     * @param task the task to be executed in the task loops
     */
    public void offerTask(Task task) {
        synchronized (taskLoops_) {
            for (L loop : taskLoops_) {
                loop.offer(task);
            }
        }
    }

    /**
     * Assigns a loop which weight is under the threshold even if a specified selection is added,
     * or a minimum among the task loops.
     * @param selection the selection added to a selected task loop
     * @return the task loop
     */
    protected L assign(TaskSelection selection) {
        Objects.requireNonNull(selection, "selection");

        int minCount = Integer.MAX_VALUE;
        L minCountLoop = null;
        synchronized (taskLoops_) {
            sweepDeadLoop();
            for (L loop : taskLoops_) {
                if (loop.countUpIfContains(selection)) {
                    logger_.debug("[assign] [{}] is already assigned to [{}]", selection, loop);
                    return loop;
                }
                int count = loop.selectionCount();
                if (count < minCount) {
                    minCount = count;
                    minCountLoop = loop;
                }
            }
        }
        if (minCountLoop == null) {
            throw new IllegalStateException("This TaskLoopGroup may be closed.");
        }
        minCountLoop.accept(selection);
        logger_.debug("[assign] {} is assigned to {}", selection, minCountLoop);
        return minCountLoop;
    }

    private L addTaskLoop() {
        L newTaskLoop = newTaskLoop();
        executor_.execute(newTaskLoop);
        taskLoops_.add(newTaskLoop);
        logger_.debug("[addTaskLoop] New task loop {} is created. Task loop count: {}.", taskLoops_.size());
        return newTaskLoop;
    }

    private void sweepDeadLoop() {
        for (Iterator<L> iterator = taskLoops_.iterator(); iterator.hasNext();) {
            L taskLoop = iterator.next();
            if (!taskLoop.isAlive()) {
                logger_.debug("[sweepDeadLoop] Dead loop is found: {}", taskLoop);
                iterator.remove();
            }
        }

        int min = (executor_ != null) ? executor_.getCorePoolSize() : 0;
        int n = min - taskLoops_.size();
        for (int i = 0; i < n; i++) {
            addTaskLoop();
        }
    }

    /**
     * Returns the number of task loops.
     * @return the number of task loops
     */
    public int pooledTaskLoops() {
        synchronized (taskLoops_) {
            return taskLoops_.size();
        }
    }

    /**
     * Returns a view of the task loops.
     * @return a view of the task loops
     */
    protected List<L> taskLoopsView() {
        List<L> view;
        synchronized (taskLoops_) {
            view = new ArrayList<L>(taskLoops_);
        }
        return view;
    }

    /**
     * Creates a new task loop.
     * @return a new task loop
     */
    protected abstract L newTaskLoop();
}
