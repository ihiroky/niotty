package net.ihiroky.niotty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadFactory;

/**
 * Created on 13/01/10, 18:43
 *
 * @author Hiroki Itoh
 */
public abstract class TaskLoopGroup<L extends TaskLoop<L>> {

    private Collection<L> eventLoops_ = nullValue();
    private Logger logger_ = LoggerFactory.getLogger(TaskLoopGroup.class);

    public synchronized void open(ThreadFactory threadFactory, int numberOfWorker) {
        if (!isInitialized(eventLoops_)) {
            L[] loops = newArray(numberOfWorker);
            for (int i = 0; i < numberOfWorker; i++) {
                L loop = newEventLoop();
                loop.openWith(threadFactory);
                loops[i] = loop;
                logger_.info("start event loop {}.", loop);
            }
            eventLoops_ = Collections.unmodifiableCollection(new CopyOnWriteArrayList<>(loops));
        }
    }

    public synchronized void close() {
        if (isInitialized(eventLoops_)) {
            for (L loop : eventLoops_) {
                loop.close();
            }
            eventLoops_ = nullValue();
        }
    }

    public boolean isOpen() {
        return isInitialized(eventLoops_);
    }

    public void offerTask(TaskLoop.Task<L> task) {
        for (L loop : eventLoops_) {
            loop.offerTask(task);
        }
    }

    protected final L searchLowMemberCountLoop() {
        int min = Integer.MAX_VALUE;
        L target = null;
        for (L loop : eventLoops_) {
            int registered = loop.processingMemberCount_.get();
            if (registered < min) {
                min = registered;
                target = loop;
            }
        }
        return target;
    }

    protected List<L> sortedLoopsView() {
        List<L> view = new ArrayList<>(eventLoops_);
        Collections.sort(view);
        return view;
    }

    protected abstract L newEventLoop();

    private static final Collection<TaskLoop<?>> NULL = Collections.emptyList();

    private <L> boolean isInitialized(Collection<L> c) {
        return c != NULL;
    }

    @SuppressWarnings("unchecked")
    private Collection<L> nullValue() {
        return (Collection<L>) NULL;
    }

    @SuppressWarnings("unchecked")
    private static <L extends TaskLoop<L>> L[] newArray(int size) {
        return (L[]) new TaskLoop<?>[size];
    }
}
