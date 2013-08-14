package net.ihiroky.niotty;

import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

/**
 * @author Hiroki Itoh
 */
public abstract class AbstractTransportFuture implements TransportFuture {

    private volatile TransportFutureListener listener_ = NULL_LISTENER;
    private final AbstractTransport<?> transport_;

    private static final TransportFutureListener NULL_LISTENER = new NullListener();

    protected AbstractTransportFuture(AbstractTransport<?> transport) {
        transport_ = transport;
    }


    @Override
    public Transport transport() {
        return transport_;
    }

    @Override
    public final TransportFuture addListener(final TransportFutureListener listener) {
        Objects.requireNonNull(listener, "listener");

        TaskLoop taskLoop = transport_.taskLoop();
        synchronized (this) {
            if (!isDone()) {
                TransportFutureListener old = listener_;
                if (old == null) {
                    listener_ = listener;
                    return this;
                }
                if (old instanceof ListenerList) {
                    ((ListenerList) old).list_.add(listener);
                    return this;
                }
                ListenerList listenerList = new ListenerList();
                listenerList.list_.add(old);
                listenerList.list_.add(listener);
                listener_ = listenerList;
                return this;
            }
        }

        if (taskLoop.isInLoopThread()) {
            listener.onComplete(this);
        } else {
            @SuppressWarnings("unchecked")
            TaskLoop.Task task = new TaskLoop.Task() {
                @Override
                public long execute(TimeUnit timeUnit) throws Exception {
                    listener.onComplete(AbstractTransportFuture.this);
                    return TaskLoop.DONE;
                }
            };
            taskLoop.offerTask(task);
        }

        return this;
    }

    @Override
    public final TransportFuture removeListener(TransportFutureListener listener) {
        Objects.requireNonNull(listener, "listener");

        synchronized (this) {
            if (listener_ == listener) {
                listener_ = NULL_LISTENER;
                return this;
            }
            if (listener_ instanceof ListenerList) {
                ListenerList listenerList = (ListenerList) listener_;
                listenerList.list_.remove(listener);
                if (listenerList.list_.size() == 1) {
                    listener_ = listenerList.list_.get(0);
                }
            }
        }
        return this;
    }

    /**
     * Executes {@code onComplete(this)} for added {@code TransportFutureListener}s.
     */
    protected final void fireOnComplete() {
        TaskLoop taskLoop = transport_.taskLoop();
        if (taskLoop.isInLoopThread()) {
            listener_.onComplete(this);
        } else {
            taskLoop.offerTask(new TaskLoop.Task() {
                @Override
                public long execute(TimeUnit timeUnit) throws Exception {
                    listener_.onComplete(AbstractTransportFuture.this);
                    return TaskLoop.DONE;
                }
            });
        }
    }

    private static class NullListener implements TransportFutureListener {
        @Override
        public void onComplete(TransportFuture future) {
        }
    }

    /**
     * An aggregator for TransportListener.
     */
    private static class ListenerList implements TransportFutureListener {

        CopyOnWriteArrayList<TransportFutureListener> list_ = new CopyOnWriteArrayList<>();

        @Override
        public void onComplete(TransportFuture future) {
            for (TransportFutureListener listener : list_) {
                listener.onComplete(future);
            }
        }
    }
}
