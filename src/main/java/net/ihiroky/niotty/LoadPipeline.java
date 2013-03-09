package net.ihiroky.niotty;

/**
 * Created on 13/01/09, 17:21
 *
 * @author Hiroki Itoh
 */
public interface LoadPipeline extends Pipeline {

    LoadPipeline add(LoadStage<?, ?> stage);
    void fire(Object input);
    void fire(TransportStateEvent event);
}
