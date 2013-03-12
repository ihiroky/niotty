package net.ihiroky.niotty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
public abstract class EventLoop<L extends EventLoop<L>> implements Runnable, Comparable<EventLoop<L>> {

    private Queue<Task<L>> taskQueue_;
    private volatile Thread thread_;

    protected final AtomicInteger processingMemberCount_ = new AtomicInteger();

    private Logger logger_ = LoggerFactory.getLogger(EventLoop.class);

    private static final int TIMEOUT_TASK_RETRY = 100;
    private static final int TIMEOUT_NOW = 0;
    private static final int TIMEOUT_NO_LIMIT = -1;

    protected EventLoop() {
        taskQueue_ = new ConcurrentLinkedQueue<Task<L>>();
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
        int timeout = 0;
        try {
            while (thread_ != null) {
                try {
                    process(timeout);
                } catch (Exception e) {
                    if (thread_ != null) {
                        logger_.warn("[run] process failed.", e);
                    }
                }
                timeout = processTasks(taskQueue_, taskBuffer);
            }
        } finally {
            taskQueue_.clear();
            onClose();
            synchronized (this) {
                thread_ = null;
            }
        }
    }

    private int processTasks(Queue<Task<L>> queue, Queue<Task<L>> buffer) {
        @SuppressWarnings("unchecked") L loop = (L) this;
        for (Task<L> task;;) {
            task = queue.poll();
            if (task == null) {
                break;
            }
            try {
                if (!task.execute(loop)) {
                    buffer.offer(task);
                }
            } catch (Exception e) {
                if (thread_ != null) {
                    logger_.warn("failed to execute task : " + task, e);
                }
            }
        }
        int timeout = buffer.isEmpty() ? TIMEOUT_NO_LIMIT : TIMEOUT_TASK_RETRY;
        queue.addAll(buffer);
        buffer.clear();
        return timeout;
    }

    public int processingMemberCount() {
        return processingMemberCount_.get();
    }

    @Override
    public String toString() {
        return (thread_ != null) ? thread_.getName() : "[NOT WORKING]";
    }

    @Override
    public int compareTo(EventLoop<L> that) {
        return this.processingMemberCount_.get() - that.processingMemberCount_.get();
    }

    protected abstract void onOpen();
    protected abstract void onClose();
    protected abstract void process(int timeout) throws Exception;
    protected abstract void wakeUp();

    public interface Task<L extends EventLoop<L>> {
        boolean execute(L eventLoop) throws Exception;
    }
}
