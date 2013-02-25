package net.ihiroky.niotty;

import net.ihiroky.niotty.event.MessageEvent;
import net.ihiroky.niotty.event.TransportStateEvent;

/**
 * Created on 13/01/09, 17:23
 *
 * @author Hiroki Itoh
 */
public interface StoreStage<I, O> {

    void store(StoreStageContext<I, O> context, MessageEvent<I> event);
    void store(StoreStageContext<I, O> context, TransportStateEvent event);
}
