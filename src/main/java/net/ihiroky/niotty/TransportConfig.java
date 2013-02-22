package net.ihiroky.niotty;

import java.util.Objects;

/**
 * Created on 13/01/15, 18:27
 *
 * @author Hiroki Itoh
 */
public class TransportConfig {

    private volatile PipelineInitializer pipelineInitializer = EMPTY;

    private static final PipelineInitializer EMPTY = new EmptyPipelineInitializer();

    public PipelineInitializer getPipelineInitializer() {
        return pipelineInitializer;
    }

    public void setPipelineInitializer(PipelineInitializer pipelineInitializer) {
        Objects.requireNonNull(pipelineInitializer, "pipelineInitializer");
        this.pipelineInitializer = pipelineInitializer;
    }

    private static class EmptyPipelineInitializer implements PipelineInitializer {
        @Override
        public void setUpPipeline(Pipeline loadPipeline, Pipeline storePipeline) {
        }
    }
}
