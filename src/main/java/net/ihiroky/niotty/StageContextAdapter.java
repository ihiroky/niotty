package net.ihiroky.niotty;

import net.ihiroky.niotty.event.MessageEvent;
import net.ihiroky.niotty.event.TransportStateEvent;

/**
 * Created on 13/01/18, 15:52
 *
 * @author Hiroki Itoh
 */
public class StageContextAdapter<I, O> implements StageContextListener<I, O> {
    @Override
    public void onFire(Pipeline pipeline, StageContext<I, O> context, MessageEvent<I> event) {
    }

    @Override
    public void onFire(Pipeline pipeline, StageContext<I, O> context, TransportStateEvent event) {
    }

    @Override
    public void onProceed(Pipeline pipeline, StageContext<I, O> context, MessageEvent<O> event) {
    }

    @Override
    public void onProceed(Pipeline pipeline, StageContext<I, O> context, TransportStateEvent event) {
    }
}
