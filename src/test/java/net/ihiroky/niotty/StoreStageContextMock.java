package net.ihiroky.niotty;

import net.ihiroky.niotty.event.MessageEvent;

/**
 * @author Hiroki Itoh
 */
public class StoreStageContextMock<I, O> extends StoreStageContext<I, O> {

    MessageEvent<O> proceededMessageEvent;

    @SuppressWarnings("unchecked")
    public StoreStageContextMock(StoreStage<?, ?> stage) {
        super(null, (StoreStage<Object, Object>) stage);
    }

    @Override
    public void proceed(MessageEvent<O> messageEvent) {
        proceededMessageEvent = messageEvent;
    }

    public MessageEvent<O> getProceededMessageEvent() {
        return proceededMessageEvent;
    }
}
