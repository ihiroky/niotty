package net.ihiroky.niotty;

import net.ihiroky.niotty.event.MessageEvent;
import net.ihiroky.niotty.event.TransportStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created on 13/01/09, 17:24
 *
 * @author Hiroki Itoh
 */
public class StageContext<I, O> {

    private Stage<I, O> stage;
    private StageContext<O, ?> next;
    private StageContextListener<I, O> listener;
    private Pipeline pipeline;

    private Logger logger = LoggerFactory.getLogger(StageContext.class);

    private static final StageContextListener<Object, Object> NULL_LISTENER = new StageContextAdapter<>();
    static final StageContext<Object, Object> TERMINAL = new StageContext<>(null, new NullStage());

    @SuppressWarnings("unchecked")
    StageContext(Pipeline pipeline, Stage<I, O> stage) {
        Objects.requireNonNull(stage, "stage");

        this.stage = stage;
        this.next = (StageContext<O, ?>) TERMINAL;
        this.listener = (StageContextListener<I, O>) NULL_LISTENER;
        this.pipeline = pipeline;
    }


    protected void fire(MessageEvent<I> event) {
        logger.trace("execute {} with {}", stage, event);
        listener.onFire(pipeline, this, event);
        stage.process(this, event);
    }

    protected void fire(TransportStateEvent event) {
        logger.trace("execute {} with {}", stage, event);
        listener.onFire(pipeline, this, event);
        stage.process(this, event);
    }

    public void proceed(MessageEvent<O> event) {
        listener.onProceed(pipeline, this, event);
        next.fire(event);
    }

    public void proceed(TransportStateEvent event) {
        listener.onProceed(pipeline, this, event);
        next.fire(event);
    }

    protected void setNext(StageContext<O, ?> context) {
        Objects.requireNonNull(context, "context");
        this.next = context;
    }

    protected StageContext<O, ?> getNext() {
        return next;
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
            ((ListenerList<I, O>)listener).list.add(newListener);
            return;
        }

        ListenerList<I, O> listenerList = new ListenerList<>();
        listenerList.list.add(oldListener);
        listenerList.list.add(newListener);
        listener = listenerList;
    }

    public Stage<I, O> getStage() {
        return stage;
    }

    @Override
    public String toString() {
        return "stage:" + stage;
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

    private static class NullStage implements Stage<Object, Object> {
        @Override
        public void process(StageContext<Object, Object> context, MessageEvent<Object> event) {
        }
        @Override
        public void process(StageContext<Object, Object> context, TransportStateEvent event) {
        }
        @Override
        public String toString() {
            return "NULL_STAGE";
        }
    }
}
