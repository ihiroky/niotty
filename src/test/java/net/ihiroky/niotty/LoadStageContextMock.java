package net.ihiroky.niotty;

import net.ihiroky.niotty.event.MessageEvent;

import java.util.ArrayDeque;
import java.util.Queue;

/**
 * @author Hiroki Itoh
 */
public class LoadStageContextMock<I, O> extends LoadStageContext<I, O> {

    Queue<MessageEvent<O>> proceededMessageEventQueue = new ArrayDeque<>();

    @SuppressWarnings("unchecked")
    public LoadStageContextMock(LoadStage<?, ?> stage) {
        super(null, (LoadStage<Object, Object>) stage);
    }

    @Override
    public void proceed(MessageEvent<O> messageEvent) {
        proceededMessageEventQueue.add(messageEvent);
    }

    public Queue<MessageEvent<O>> getProceededMessageEventQueue() {
        return proceededMessageEventQueue;
    }
}
