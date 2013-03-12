package net.ihiroky.niotty;

/**
 * Created on 13/01/09, 17:21
 *
 * @author Hiroki Itoh
 */
public interface LoadPipeline extends Pipeline {

    LoadPipeline add(LoadStage<?, ?> stage);
    LoadPipeline add(LoadStage<?, ?> stage, StageContextExecutor<?> executor);
    void execute(Object input);
    void execute(TransportStateEvent event);
    void close();
}
