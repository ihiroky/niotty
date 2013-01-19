package net.ihiroky.niotty;

import net.ihiroky.niotty.event.MessageEvent;
import net.ihiroky.niotty.event.TransportStateEvent;

/**
 * Created on 13/01/18, 15:52
 *
 * @author Hiroki Itoh
 */
public class StageContextAdapter<E> implements StageContextListener<E> {
    @Override
    public void onFire(PipeLine pipeLine, StageContext context, MessageEvent<E> event) {
    }

    @Override
    public void onFire(PipeLine pipeLine, StageContext context, TransportStateEvent event) {
    }

    @Override
    public void onProceed(PipeLine pipeLine, StageContext context, MessageEvent<E> event) {
    }

    @Override
    public void onProceed(PipeLine pipeLine, StageContext context, TransportStateEvent event) {
    }
}
