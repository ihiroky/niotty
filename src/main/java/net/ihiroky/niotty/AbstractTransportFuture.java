package net.ihiroky.niotty;

import net.ihiroky.niotty.util.Arguments;

import java.util.concurrent.CopyOnWriteArrayList;

/**
 *
 */
public abstract class AbstractTransportFuture implements TransportFuture {

    private volatile CompletionListener listener_ = NULL_LISTENER;
    private final Transport transport_;

    private static final CompletionListener NULL_LISTENER = new NullListener();

    /**
     * Create a new instance.
     * @param transport a transport associated with this future
     */
    protected AbstractTransportFuture(Transport transport) {
        transport_ = transport;
    }


    @Override
    public Transport transport() {
        return transport_;
    }

    @Override
    public final TransportFuture addListener(final CompletionListener listener) {
        Arguments.requireNonNull(listener, "listener");

        EventDispatcher eventDispatcher = transport_.eventDispatcher();
        synchronized (this) {
            if (!isDone()) {
                CompletionListener old = listener_;
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

        if (eventDispatcher.isInDispatcherThread()) {
            listener.onComplete(this);
        } else {
            @SuppressWarnings("unchecked")
            Event event = new Event() {
                @Override
                public long execute() throws Exception {
                    listener.onComplete(AbstractTransportFuture.this);
                    return DONE;
                }
            };
            eventDispatcher.offer(event);
        }

        return this;
    }

    @Override
    public final TransportFuture removeListener(CompletionListener listener) {
        Arguments.requireNonNull(listener, "listener");

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
     * Executes {@code onComplete(this)} for added {@code CompletionListener}s.
     */
    protected final void fireOnComplete() {
        EventDispatcher eventDispatcher = transport_.eventDispatcher();
        if (eventDispatcher.isInDispatcherThread()) {
            listener_.onComplete(this);
        } else {
            eventDispatcher.offer(new Event() {
                @Override
                public long execute() throws Exception {
                    listener_.onComplete(AbstractTransportFuture.this);
                    return DONE;
                }
            });
        }
    }

    private static class NullListener implements CompletionListener {
        @Override
        public void onComplete(TransportFuture future) {
        }
    }

    /**
     * An aggregator for TransportListener.
     */
    private static class ListenerList implements CompletionListener {

        CopyOnWriteArrayList<CompletionListener> list_ = new CopyOnWriteArrayList<CompletionListener>();

        @Override
        public void onComplete(TransportFuture future) {
            for (CompletionListener listener : list_) {
                listener.onComplete(future);
            }
        }
    }
}
