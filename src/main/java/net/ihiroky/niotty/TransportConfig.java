package net.ihiroky.niotty;

import java.util.Objects;

/**
 * Created on 13/01/15, 18:27
 *
 * @author Hiroki Itoh
 */
public class TransportConfig {

    private volatile PipelineInitializer pipelineInitializer_ = EMPTY;

    private static final PipelineInitializer EMPTY = new EmptyPipelineInitializer();

    public PipelineInitializer getPipelineInitializer() {
        return pipelineInitializer_;
    }

    public void setPipelineInitializer(PipelineInitializer pipelineInitializer) {
        Objects.requireNonNull(pipelineInitializer, "pipelineInitializer");
        this.pipelineInitializer_ = pipelineInitializer;
    }

    private static class EmptyPipelineInitializer implements PipelineInitializer {
        @Override
        public void setUpPipeline(LoadPipeline loadPipeline, StorePipeline storePipeline) {
        }
    }
}
