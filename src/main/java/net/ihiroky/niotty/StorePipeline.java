package net.ihiroky.niotty;

/**
 * Created on 13/01/09, 17:21
 *
 * @author Hiroki Itoh
 */
public interface StorePipeline extends Pipeline {

    StorePipeline add(StoreStage<?, ?> stage);
    void fire(Object object);
    void fire(TransportStateEvent event);
}
