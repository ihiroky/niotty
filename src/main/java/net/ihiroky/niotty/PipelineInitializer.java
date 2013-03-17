package net.ihiroky.niotty;

/**
 * Created on 13/01/09, 18:57
 *
 * @author Hiroki Itoh
 */
public interface PipelineInitializer {

    void setUpPipeline(LoadPipeline loadPipeline, StorePipeline storePipeline);
}
