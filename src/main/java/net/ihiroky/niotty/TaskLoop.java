package net.ihiroky.niotty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.AbstractQueue;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created on 13/01/10, 17:56
 *
 * @author Hiroki Itoh
 */
public abstract class TaskLoop<L extends TaskLoop<L>> implements Runnable, Comparable<TaskLoop<L>> {

    private volatile Queue<Task<L>> taskQueue_;
    private volatile Thread thread_;

    protected final AtomicInteger processingMemberCount_ = new AtomicInteger();

    private Logger logger_ = LoggerFactory.getLogger(TaskLoop.class);

    public static final int TIMEOUT_NO_LIMIT = -1;
    public static final int TIMEOUT_NOW = 0;

    protected TaskLoop() {
        taskQueue_ = new ConcurrentLinkedQueue<>();
    }

    synchronized void openWith(ThreadFactory tf) {
        Objects.requireNonNull(tf, "tf");

        onOpen();
        if (thread_ == null) {
            thread_ = tf.newThread(this);
            thread_.start();
        }
    }

    public synchronized void close() {
        Thread t = thread_;
        thread_ = null;
        if (t != null) {
            t.interrupt();
        }
    }

    public void offerTask(Task<L> task) {
        taskQueue_.offer(task);
        wakeUp();
    }

    public boolean hasNoTask() {
        return taskQueue_.isEmpty();
    }

    public void run() {
        Queue<Task<L>> taskBuffer = new LinkedList<Task<L>>();
        int waitTimeMillis = 0;
        try {
            while (thread_ != null) {
                try {
                    process(waitTimeMillis);
                } catch (Exception e) {
                    if (thread_ != null) {
                        logger_.warn("[run] process failed.", e);
                    }
                }
                waitTimeMillis = processTasks(taskQueue_, taskBuffer);
            }
        } finally {
            Queue<Task<L>> queue = taskQueue_;
            taskQueue_ = EmptyQueue.emptyQueue();
            queue.clear();
            onClose();
            synchronized (this) {
                thread_ = null;
            }
        }
    }

    private int processTasks(Queue<Task<L>> queue, Queue<Task<L>> buffer) {
        @SuppressWarnings("unchecked") L loop = (L) this;
        int minWaitTimeMillis = Integer.MAX_VALUE;
        for (Task<L> task;;) {
            task = queue.poll();
            if (task == null) {
                break;
            }
            try {
                int waitTimeMillis = task.execute(loop);
                if (waitTimeMillis >= 0) {
                    buffer.offer(task);
                    if (minWaitTimeMillis > waitTimeMillis) {
                        minWaitTimeMillis = waitTimeMillis;
                    }
                }
            } catch (Exception e) {
                if (thread_ != null) {
                    logger_.warn("failed to execute task : " + task, e);
                }
            }
        }
        if (minWaitTimeMillis == Integer.MAX_VALUE) {
            return TIMEOUT_NO_LIMIT;
        } else {
            queue.addAll(buffer);
            buffer.clear();
            return minWaitTimeMillis;
        }
    }

    public int processingMemberCount() {
        return processingMemberCount_.get();
    }

    protected boolean isInLoopThread() {
        return Thread.currentThread() == thread_;
    }

    @Override
    public String toString() {
        return (thread_ != null) ? thread_.getName() : "[NOT WORKING]";
    }

    @Override
    public int compareTo(TaskLoop<L> that) {
        return this.processingMemberCount_.get() - that.processingMemberCount_.get();
    }

    protected abstract void onOpen();
    protected abstract void onClose();
    protected abstract void process(int waitTimeMillis) throws Exception;
    protected abstract void wakeUp();

    public interface Task<L extends TaskLoop<L>> {
        int execute(L eventLoop) throws Exception;
    }

    private static class EmptyQueue extends AbstractQueue<Object> {

        private static final EmptyQueue INSTANCE = new EmptyQueue();

        @SuppressWarnings("unchecked")
        static <E> Queue<E> emptyQueue() {
            return (Queue<E>) INSTANCE;
        }

        @Override
        public Iterator<Object> iterator() {
            return Collections.emptyIterator();
        }

        @Override
        public int size() {
            return 0;
        }

        @Override
        public boolean offer(Object task) {
            return false;
        }

        @Override
        public Object poll() {
            return null;
        }

        @Override
        public Object peek() {
            return null;
        }
    }
}
