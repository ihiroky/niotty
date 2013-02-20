package net.ihiroky.niotty;

import net.ihiroky.niotty.event.MessageEvent;

/**
 * @author Hiroki Itoh
 */
public class StageContextMock<I, O> extends StageContext<I, O> {

    MessageEvent<O> proceededMessageEvent;

    public StageContextMock(Stage<I, O> stage) {
        super(null, stage);
    }

    @Override
    public void proceed(MessageEvent<O> messageEvent) {
        proceededMessageEvent = messageEvent;
    }

    public MessageEvent<O> getProceededMessageEvent() {
        return proceededMessageEvent;
    }
}
