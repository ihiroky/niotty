package net.ihiroky.niotty;

import net.ihiroky.niotty.event.MessageEvent;
import net.ihiroky.niotty.event.TransportStateEvent;

/**
 * Created on 13/01/09, 17:23
 *
 * @author Hiroki Itoh
 */
public interface LoadStage<I, O> {

    void load(LoadStageContext<I, O> context, MessageEvent<I> event);
    void load(LoadStageContext<I, O> context, TransportStateEvent event);
}
