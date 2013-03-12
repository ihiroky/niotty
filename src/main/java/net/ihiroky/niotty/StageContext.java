package net.ihiroky.niotty;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created on 13/01/09, 17:24
 *
 * @author Hiroki Itoh
 */
public abstract class StageContext<I, O> {

    private Pipeline pipeline_;
    private StageContext<O, Object> next_;
    private StageContextExecutor<I> executor_;
    private StageContextListener<I, O> listener_;

    private static final StageContextListener<Object, Object> NULL_LISTENER = new StageContextAdapter<>();
    private static final StageContextExecutor<Object> DEFAULT_EXECUTOR = new DefaultStageContextExecutor();

    @SuppressWarnings("unchecked")
    private static <I, O> StageContextListener<I, O> nullListener() {
        return (StageContextListener<I, O>) NULL_LISTENER;
    }

    protected StageContext(Pipeline pipeline, StageContextExecutor<I> executor) {
        @SuppressWarnings("unchecked")
        StageContextExecutor<I> e = (executor != null) ? executor : (StageContextExecutor<I>) DEFAULT_EXECUTOR;
        this.pipeline_ = pipeline;
        this.listener_ = nullListener();
        this.executor_ = e;
    }

    protected StageContext<O, Object> next() {
        return next_;
    }

    protected void setNext(StageContext<O, Object> next) {
        Objects.requireNonNull(next, "next");
        this.next_ = next;
    }

    public Transport transport() {
        return pipeline_.transport();
    }

    protected void close() {
        listener_ = nullListener();
        executor_.invalidate(this);
    }

    protected void execute(I input) {
        executor_.execute(this, input);
    }

    protected void execute(TransportStateEvent event) {
        executor_.execute(this, event);
    }

    public void proceed(O output) {
        listener_.onProceed(pipeline_, this, output);
        next_.execute(output);
    }

    public void proceed(TransportStateEvent event) {
        listener_.onProceed(pipeline_, this, event);
        next_.execute(event);
    }

    protected void callOnFire(I input) {
        listener_.onFire(pipeline_, this, input);
    }

    protected void callOnFire(TransportStateEvent event) {
        listener_.onFire(pipeline_, this, event);
    }

    public void addListener(StageContextListener<?, ?> contextListener) {
        Objects.requireNonNull(contextListener, "contextListener");

        StageContextListener<I, O> oldListener = listener_;
        @SuppressWarnings("unchecked")
        StageContextListener<I, O> newListener = (StageContextListener<I, O>) contextListener;

        if (listener_ == NULL_LISTENER) {
            listener_ = newListener;
            return;
        }
        if (listener_ instanceof ListenerList) {
            ((ListenerList<I, O>) listener_).list.add(newListener);
            return;
        }

        ListenerList<I, O> listenerList = new ListenerList<>();
        listenerList.list.add(oldListener);
        listenerList.list.add(newListener);
        listener_ = listenerList;
    }

    protected StageContextListener<I, O> getListener() {
        return listener_;
    }

    private static class ListenerList<I, O> implements StageContextListener<I, O> {

        List<StageContextListener<I, O>> list = new CopyOnWriteArrayList<>();

        @Override
        public void onFire(Pipeline pipeline, StageContext<I, O> context, I input) {
            for (StageContextListener<I, O> listener : list) {
                listener.onFire(pipeline, context, input);
            }
        }

        @Override
        public void onFire(Pipeline pipeline, StageContext<I, O> context, TransportStateEvent event) {
            for (StageContextListener<I, O> listener : list) {
                listener.onFire(pipeline, context, event);
            }
        }

        @Override
        public void onProceed(Pipeline pipeline, StageContext<I, O> context, O output) {
            for (StageContextListener<I, O> listener : list) {
                listener.onProceed(pipeline, context, output);
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
    protected abstract void fire(I input);
    protected abstract void fire(TransportStateEvent event);
}
