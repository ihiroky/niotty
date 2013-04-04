package net.ihiroky.niotty;

import java.util.ArrayDeque;
import java.util.Queue;

/**
 * @author Hiroki Itoh
 */
public class LoadStageContextMock<I, O> extends LoadStageContext<I, O> {

    Queue<O> proceededMessageEventQueue_ = new ArrayDeque<>();

    @SuppressWarnings("unchecked")
    public LoadStageContextMock(LoadStage<?, ?> stage) {
        super(null, null, (LoadStage<Object, Object>) stage, null);
    }

    @Override
    public void proceed(O messageEvent) {
        proceededMessageEventQueue_.add(messageEvent);
    }

    public Queue<O> getProceededMessageEventQueue() {
        return proceededMessageEventQueue_;
    }
}
