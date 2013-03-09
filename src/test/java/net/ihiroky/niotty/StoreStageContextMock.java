package net.ihiroky.niotty;

import java.util.ArrayDeque;
import java.util.Queue;

/**
 * @author Hiroki Itoh
 */
public class StoreStageContextMock<I, O> extends StoreStageContext<I, O> {

    Queue<O> proceededMessageEvent_ = new ArrayDeque<>();

    @SuppressWarnings("unchecked")
    public StoreStageContextMock(StoreStage<?, ?> stage) {
        super(null, (StoreStage<Object, Object>) stage);
    }

    @Override
    public void proceed(O messageEvent) {
        proceededMessageEvent_.offer(messageEvent);
    }

    public Queue<O> getProceededMessageEvent() {
        return proceededMessageEvent_;
    }
}
