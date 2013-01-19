package net.ihiroky.niotty;

import net.ihiroky.niotty.event.MessageEvent;
import net.ihiroky.niotty.event.TransportStateEvent;

/**
 * Created on 13/01/18, 15:46
 *
 * @author Hiroki Itoh
 */
public class StageAdapter<E> implements Stage<E> {
    @Override
    public void process(StageContext context, MessageEvent<E> event) {
        context.proceed(event);
    }

    @Override
    public void process(StageContext context, TransportStateEvent event) {
        context.proceed(event);
    }
}
