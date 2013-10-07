package net.ihiroky.niotty;

import net.ihiroky.niotty.util.Arguments;
import net.ihiroky.niotty.util.Closable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ThreadFactory;

/**
 * Provides a thread pool to execute {@link TaskLoop} and manages the task loop lifecycle.
 *
 * @param <L> the actual type of the TaskLoop
 * @author Hiroki Itoh
 */
public abstract class TaskLoopGroup<L extends TaskLoop> implements Closable {

    private final Collection<L> taskLoops_;
    private final ThreadFactory threadFactory_;
    private final int workers_;
    private Logger logger_ = LoggerFactory.getLogger(TaskLoopGroup.class);

    /**
     * Constructs a new instance.
     *
     * @param threadFactory a factory to create thread which runs a task loop
     * @param workers the number of threads held in the thread pool
     */
    protected TaskLoopGroup(ThreadFactory threadFactory, int workers) {
        taskLoops_ = new HashSet<L>();
        threadFactory_ = Arguments.requireNonNull(threadFactory, "threadFactory");
        workers_ = Arguments.requirePositive(workers, "workers");
    }

    /**
     * Cleans up and Sets up the thread pool and if not created.
     * This method may as well bing called ahead.
     */
    public final void open() {
        List<L> newTaskLoopList;
        synchronized (taskLoops_) {
            for (Iterator<L> iterator = taskLoops_.iterator(); iterator.hasNext();) {
                L taskLoop = iterator.next();
                if (!taskLoop.isAlive()) {
                    logger_.debug("[open0] Dead task loop {} is found. Remove it and assign a new one.", taskLoop);
                    iterator.remove();
                }
            }

            int n = workers_ - taskLoops_.size();
            if (n == 0) {
                return;
            }
            newTaskLoopList = new ArrayList<L>(n);
            for (int i = 0; i < n; i++) {
                L newTaskLoop = newTaskLoop();
                Thread thread = threadFactory_.newThread(newTaskLoop);
                thread.start();
                taskLoops_.add(newTaskLoop);
                newTaskLoopList.add(newTaskLoop);
                logger_.debug("[open0] New task loop {} is created. Task loop count: {}.",
                        newTaskLoop, taskLoops_.size());
            }
        }

        // Ensure that loop.onOpen() is called now.
        // And doesn't wait in the synchronized block.
        try {
            for (L newTaskLoop : newTaskLoopList) {
                newTaskLoop.waitUntilStarted();
            }
        } catch (InterruptedException ie) {
            logger_.debug("[open0] Interrupted. Close active task loops.", ie);
            close();
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
        }
    }

    /**
     * Returns true if the thread pool is available.
     * @return true if the thread pool is available
     */
    public boolean isOpen() {
        synchronized (taskLoops_) {
            return !taskLoops_.isEmpty();
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
    public L assign(TaskSelection selection) {
        Arguments.requireNonNull(selection, "selection");

        int minCount = Integer.MAX_VALUE;
        L minCountLoop = null;
        open();
        synchronized (taskLoops_) {
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

    /**
     * Creates a new task loop.
     *
     * @return the task loop
     */
    protected abstract L newTaskLoop();
}
