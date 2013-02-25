package net.ihiroky.niotty;

import net.ihiroky.niotty.event.MessageEvent;

/**
 * @author Hiroki Itoh
 */
public class LoadStageContextMock<I, O> extends LoadStageContext<I, O> {

    MessageEvent<O> proceededMessageEvent;

    @SuppressWarnings("unchecked")
    public LoadStageContextMock(LoadStage<?, ?> stage) {
        super(null, (LoadStage<Object, Object>) stage);
    }

    @Override
    public void proceed(MessageEvent<O> messageEvent) {
        proceededMessageEvent = messageEvent;
    }

    public MessageEvent<O> getProceededMessageEvent() {
        return proceededMessageEvent;
    }
}
