package net.ihiroky.niotty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
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
public abstract class TaskLoopGroup<L extends TaskLoop<L>> {

    private final Collection<L> taskLoops_;
    private ThreadPoolExecutor executor_;
    private Logger logger_ = LoggerFactory.getLogger(TaskLoopGroup.class);
    private int taskWeightThreshold_;

    /**
     * Constructs a new instance.
     */
    protected TaskLoopGroup() {
        taskLoops_ = new HashSet<>();
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
        open(threadFactory, workers, workers);
    }

    /**
     * Creates the thread pool internally if not created.
     *
     * @param threadFactory a thread factory
     * @param minWorkers a minimum number of threads held in the thread pool
     * @param maxWorkers a maximum number of threads held in the thread pool
     * @throws IllegalArgumentException if the threadFactory is null,
     *                                  the minWorkers and the maxWorkers is not positive
     *                                  or the minWorkers is greater than the maxWorkers
     */
    public void open(ThreadFactory threadFactory, int minWorkers, int maxWorkers) {
        Objects.requireNonNull(threadFactory, "threadFactory");
        if (minWorkers <= 0) {
            throw new IllegalArgumentException("minWorkers must be positive.");
        }
        if (maxWorkers <= 0) {
            throw new IllegalArgumentException("minWorkers must be positive.");
        }
        if (minWorkers > maxWorkers) {
            throw new IllegalArgumentException("maxWorkers must be equal or greater than minWorkers.");
        }

        synchronized (taskLoops_) {
            if (executor_ == null) {
                ThreadPoolExecutor executor = new ThreadPoolExecutor(
                        minWorkers, maxWorkers, 1L, TimeUnit.MINUTES, new SynchronousQueue<Runnable>(), threadFactory);
                Collection<L> taskLoops = new ArrayList<>(minWorkers);
                for (int i = 0; i < minWorkers; i++) {
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
    public void offerTask(TaskLoop.Task<L> task) {
        synchronized (taskLoops_) {
            for (L loop : taskLoops_) {
                loop.offerTask(task);
            }
        }
    }

    /**
     * Returns the weight threshold to choose a task loop.
     * @return the weight threshold to choose a task loop
     */
    public int getTaskWeightThreshold() {
        return taskWeightThreshold_;
    }

    /**
     * Sets the weight threshold to choose a task loop.
     * @param taskWeightThreshold the threshold
     */
    public void setTaskWeightThreshold(int taskWeightThreshold) {
        if (taskWeightThreshold < 0) {
            throw new IllegalArgumentException("The taskWeightThreshold must not be negative");
        }
        taskWeightThreshold_ = taskWeightThreshold;
    }

    /**
     * Assigns a loop which weight is under the threshold even if a specified selection is added,
     * or a minimum among the task loops.
     * @param selection the selection added to a selected task loop
     * @return the task loop
     */
    protected L assign(TaskSelection selection) {
        Objects.requireNonNull(selection, "selection");
        if (selection.weight() <= 0) {
            throw new IllegalArgumentException("The selection.weight() must be positive.");
        }

        int minWeight = Integer.MAX_VALUE;
        L minWeighted = null;
        int minWeightedUnderThreshold = Integer.MAX_VALUE;
        L weightedUnderThreshold = null;
        synchronized (taskLoops_) {
            sweepDeadLoop();
            for (L loop : taskLoops_) {
                if (loop.contains(selection)) {
                    logger_.debug("[assign] [{}] is already assigned to [{}]", selection, loop);
                    return loop;
                }

                int taskLoopWeight = loop.weight();
                if (taskLoopWeight < minWeight) {
                    minWeight = taskLoopWeight;
                    minWeighted = loop;
                }

                // Search already weighted task loops which is light enough to add the selection
                // and has minimum weight in the loops.
                if (taskLoopWeight > 0
                        && taskWeightThreshold_ - taskLoopWeight >= selection.weight()
                        && taskLoopWeight < minWeightedUnderThreshold) {
                    minWeightedUnderThreshold = taskLoopWeight;
                    weightedUnderThreshold = loop;
                }
            }
            if (weightedUnderThreshold != null) {
                weightedUnderThreshold.accept(selection);
                return weightedUnderThreshold;
            }
            if (taskWeightThreshold_ > 0
                    && taskLoops_.size() < executor_.getMaximumPoolSize()) {
                weightedUnderThreshold = addTaskLoop();
                weightedUnderThreshold.accept(selection);
                return weightedUnderThreshold;
            }
            if (minWeighted != null) {
                minWeighted.accept(selection);
                return minWeighted;
            }
        }

        throw new IllegalStateException("This TaskLoopGroup may be closed.");
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
            view = new ArrayList<>(taskLoops_);
        }
        return view;
    }

    /**
     * Creates a new task loop.
     * @return a new task loop
     */
    protected abstract L newTaskLoop();
}
