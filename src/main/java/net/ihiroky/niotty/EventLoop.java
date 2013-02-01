package net.ihiroky.niotty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadFactory;

/**
 * Created on 13/01/10, 17:56
 *
 * @author Hiroki Itoh
 */
public abstract class EventLoop<L extends EventLoop<L>> implements Runnable {

    private Queue<Task<L>> taskQueue;
    private volatile Thread thread;

    private Logger logger = LoggerFactory.getLogger(EventLoop.class);

    private static final int TIMEOUT = 100;

    protected EventLoop() {
        taskQueue = new ConcurrentLinkedQueue<Task<L>>();
    }

    synchronized void openWith(ThreadFactory tf) {
        Objects.requireNonNull(tf, "tf");

        onOpen();
        if (thread == null) {
            thread = tf.newThread(this);
            thread.start();
        }
    }

    public synchronized void close() {
        Thread t = thread;
        thread = null;
        if (t != null) {
            t.interrupt();
        }
    }

    public void offerTask(Task<L> task) {
        taskQueue.offer(task);
        wakeUp();
    }

    public void run() {
        Queue<Task<L>> taskBuffer = new LinkedList<Task<L>>();
        int timeout = 0;
        try {
            while (thread != null) {
                try {
                    process(timeout);
                } catch (Exception e) {
                    if (thread != null) {
                        logger.warn("[run] process failed.", e);
                    }
                }
                timeout = processTasks(taskQueue, taskBuffer);
            }
        } finally {
            taskQueue.clear();
            onClose();
            synchronized (this) {
                thread = null;
            }
        }
    }

    private int processTasks(Queue<Task<L>> queue, Queue<Task<L>> buffer) {
        @SuppressWarnings("unchecked") L loop = (L) this;
        for (Task<L> task; (task = queue.poll()) != null; ) {
            try {
                if (!task.execute(loop)) {
                    buffer.offer(task);
                }
            } catch (Exception e) {
                if (thread != null) {
                    logger.warn("failed to execute task : " + task, e);
                }
            }
        }
        int timeout = buffer.isEmpty() ? 0 : TIMEOUT;
        queue.addAll(buffer);
        buffer.clear();
        return timeout;
    }

    @Override
    public String toString() {
        return (thread != null) ? thread.getName() : "[NOT WORKING]";
    }

    protected abstract void onOpen();
    protected abstract void onClose();
    protected abstract void process(int timeout) throws Exception;
    protected abstract void wakeUp();

    public interface Task<L extends EventLoop<L>> {
        boolean execute(L eventLoop);
    }
}
