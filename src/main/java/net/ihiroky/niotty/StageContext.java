package net.ihiroky.niotty;

import net.ihiroky.niotty.event.MessageEvent;
import net.ihiroky.niotty.event.TransportStateEvent;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created on 13/01/09, 17:24
 *
 * @author Hiroki Itoh
 */
public abstract class StageContext<I, O> {

    private Pipeline pipeline;
    private StageContext<O, Object> next;
    private StageContextListener<I, O> listener;

    private static final StageContextListener<Object, Object> NULL_LISTENER = new StageContextAdapter<>();

    @SuppressWarnings("unchecked")
    private static <I, O> StageContextListener<I, O> nullListener() {
        return (StageContextListener<I, O>) NULL_LISTENER;
    }

    protected StageContext(Pipeline pipeline) {
        this.pipeline = pipeline;
        this.listener = nullListener();
    }

    protected StageContext<O, Object> getNext() {
        return next;
    }

    protected void setNext(StageContext<O, Object> next) {
        Objects.requireNonNull(next, "next");
        this.next = next;
    }

    public void proceed(MessageEvent<O> event) {
        listener.onProceed(pipeline, this, event);
        next.fire(event);

    }

    public void proceed(TransportStateEvent event) {
        listener.onProceed(pipeline, this, event);
        next.fire(event);
    }

    protected void callOnFire(MessageEvent<I> event) {
        listener.onFire(pipeline, this, event);
    }

    protected void callOnFire(TransportStateEvent event) {
        listener.onFire(pipeline, this, event);
    }

    public void addListener(StageContextListener<?, ?> contextListener) {
        Objects.requireNonNull(contextListener, "contextListener");

        StageContextListener<I, O> oldListener = listener;
        @SuppressWarnings("unchecked")
        StageContextListener<I, O> newListener = (StageContextListener<I, O>) contextListener;

        if (listener == NULL_LISTENER) {
            listener = newListener;
            return;
        }
        if (listener instanceof ListenerList) {
            ((ListenerList<I, O>) listener).list.add(newListener);
            return;
        }

        ListenerList<I, O> listenerList = new ListenerList<>();
        listenerList.list.add(oldListener);
        listenerList.list.add(newListener);
        listener = listenerList;
    }

    protected StageContextListener<I, O> getListener() {
        return listener;
    }

    private static class ListenerList<I, O> implements StageContextListener<I, O> {

        List<StageContextListener<I, O>> list = new CopyOnWriteArrayList<>();

        @Override
        public void onFire(Pipeline pipeline, StageContext<I, O> context, MessageEvent<I> event) {
            for (StageContextListener<I, O> listener : list) {
                listener.onFire(pipeline, context, event);
            }
        }

        @Override
        public void onFire(Pipeline pipeline, StageContext<I, O> context, TransportStateEvent event) {
            for (StageContextListener<I, O> listener : list) {
                listener.onFire(pipeline, context, event);
            }
        }

        @Override
        public void onProceed(Pipeline pipeline, StageContext<I, O> context, MessageEvent<O> event) {
            for (StageContextListener<I, O> listener : list) {
                listener.onProceed(pipeline, context, event);
            }
        }

        @Override
        public void onProceed(Pipeline pipeline, StageContext<I, O> context, TransportStateEvent event) {
            for (StageContextListener<I, O> listener : list) {
                listener.onProceed(pipeline, context, event);
            }
        }
    }

    protected abstract Object getStage();
    protected abstract void fire(MessageEvent<I> event);
    protected abstract void fire(TransportStateEvent event);
}
