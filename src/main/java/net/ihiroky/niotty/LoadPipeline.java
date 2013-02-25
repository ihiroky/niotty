package net.ihiroky.niotty;

import net.ihiroky.niotty.event.MessageEvent;
import net.ihiroky.niotty.event.TransportStateEvent;

/**
 * Created on 13/01/09, 17:21
 *
 * @author Hiroki Itoh
 */
public interface LoadPipeline extends Pipeline {

    LoadPipeline add(LoadStage<?, ?> stage);
    void fire(MessageEvent<Object> event);
    void fire(TransportStateEvent event);
}
