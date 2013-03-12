package net.ihiroky.niotty;

/**
 * Created on 13/01/09, 17:21
 *
 * @author Hiroki Itoh
 */
public interface StorePipeline extends Pipeline {

    StorePipeline add(StoreStage<?, ?> stage);
    StorePipeline add(StoreStage<?, ?> stage, StageContextExecutor<?> executor);
    void execute(Object object);
    void execute(TransportStateEvent event);
    void close();
}
