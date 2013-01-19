package net.ihiroky.niotty;

import net.ihiroky.niotty.event.MessageEvent;
import net.ihiroky.niotty.event.TransportStateEvent;

/**
 * Created on 13/01/09, 17:23
 *
 * @author Hiroki Itoh
 */
public interface Stage<E> {

    void process(StageContext context, MessageEvent<E> event);
    void process(StageContext context, TransportStateEvent event);
}
