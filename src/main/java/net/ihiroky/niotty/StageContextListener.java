package net.ihiroky.niotty;

import net.ihiroky.niotty.event.MessageEvent;
import net.ihiroky.niotty.event.TransportStateEvent;

/**
 * Created on 13/01/17, 12:59
 *
 * @author Hiroki Itoh
 */
public interface StageContextListener<E> {

    void onFire(PipeLine pipeLine, StageContext context, MessageEvent<E> event);
    void onFire(PipeLine pipeLine, StageContext context, TransportStateEvent event);
    void onProceed(PipeLine pipeLine, StageContext context, MessageEvent<E> event);
    void onProceed(PipeLine pipeLine, StageContext context, TransportStateEvent event);
}
