package net.ihiroky.niotty;

import net.ihiroky.niotty.event.MessageEvent;
import net.ihiroky.niotty.event.TransportStateEvent;

/**
 * Created on 13/01/17, 12:59
 *
 * @author Hiroki Itoh
 */
public interface StageContextListener<I, O> {

    void onFire(Pipeline pipeline, StageContext<I, O> context, MessageEvent<I> event);
    void onFire(Pipeline pipeline, StageContext<I, O> context, TransportStateEvent event);
    void onProceed(Pipeline pipeline, StageContext<I, O> context, MessageEvent<O> event);
    void onProceed(Pipeline pipeline, StageContext<I, O> context, TransportStateEvent event);
}
