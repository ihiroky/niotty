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
public class StageContext {

    private Stage<Object> stage;
    private StageContext next;
    private StageContextListener<Object> listener;
    private PipeLine pipeLine;

    private Logger logger = LoggerFactory.getLogger(StageContext.class);

    private static final StageContextListener<Object> NULL_LISTENER = new StageContextAdapter<Object>();
    private static final Stage<Object> NULL_STAGE = new Stage<Object>() {
        @Override
        public void process(StageContext context, MessageEvent<Object> event) {
        }
        @Override
        public void process(StageContext context, TransportStateEvent event) {
        }
        @Override
        public String toString() {
            return "Null Stage";
        }
    };
    private static final StageContext TERMINAL = new StageContext(null, NULL_STAGE);

    @SuppressWarnings("unchecked")
    StageContext(PipeLine pipeLine, Stage<?> stage) {
        Objects.requireNonNull(stage, "stage");

        this.stage = (Stage<Object>) stage;
        this.next = TERMINAL;
        this.listener = NULL_LISTENER;
        this.pipeLine = pipeLine;
    }


    protected void fire(MessageEvent<?> event) {
        logger.trace("execute {} with {}", stage, event);
        @SuppressWarnings("unchecked") MessageEvent<Object> e = (MessageEvent<Object>)event;
        listener.onFire(pipeLine, this, e);
        stage.process(this, e);
    }

    protected void fire(TransportStateEvent event) {
        logger.trace("execute {} with {}", stage, event);
        listener.onFire(pipeLine, this, event);
        stage.process(this, event);
    }

    public void proceed(MessageEvent<?> event) {
        @SuppressWarnings("unchecked") MessageEvent<Object> e = (MessageEvent<Object>) event;
        listener.onProceed(pipeLine, this, e);
        next.fire(event);
    }

    public void proceed(TransportStateEvent event) {
        listener.onProceed(pipeLine, this, event);
        next.fire(event);
    }

    protected void setNext(StageContext context) {
        Objects.requireNonNull(context, "context");
        this.next = context;
    }

    protected StageContext getNext() {
        return next;
    }


    public void addListener(StageContextListener<?> contextListener) {
        Objects.requireNonNull(contextListener, "contextListener");

        @SuppressWarnings("unchecked")
        StageContextListener<Object> oldListener = listener;
        @SuppressWarnings("unchecked")
        StageContextListener<Object> newListener = (StageContextListener<Object>) contextListener;

        if (listener == NULL_LISTENER) {
            listener = newListener;
            return;
        }
        if (listener instanceof ListenerList) {
            ((ListenerList<Object>)listener).list.add(newListener);
            return;
        }

        ListenerList<Object> listenerList = new ListenerList<Object>();
        listenerList.list.add(oldListener);
        listenerList.list.add(newListener);
        listener = listenerList;
    }

    public Stage<?> getStage() {
        return stage;
    }

    @Override
    public String toString() {
        return "stage:" + stage;
    }

    private static class ListenerList<E> implements StageContextListener<E> {

        List<StageContextListener<E>> list = new CopyOnWriteArrayList<StageContextListener<E>>();

        @Override
        public void onFire(PipeLine pipeLine, StageContext context, MessageEvent<E> event) {
            for (StageContextListener<E> listener : list) {
                listener.onFire(pipeLine, context, event);
            }
        }

        @Override
        public void onFire(PipeLine pipeLine, StageContext context, TransportStateEvent event) {
            for (StageContextListener<E> listener : list) {
                listener.onFire(pipeLine, context, event);
            }
        }

        @Override
        public void onProceed(PipeLine pipeLine, StageContext context, MessageEvent<E> event) {
            for (StageContextListener<E> listener : list) {
                listener.onProceed(pipeLine, context, event);
            }
        }

        @Override
        public void onProceed(PipeLine pipeLine, StageContext context, TransportStateEvent event) {
            for (StageContextListener<E> listener : list) {
                listener.onProceed(pipeLine, context, event);
            }
        }
    }
}
