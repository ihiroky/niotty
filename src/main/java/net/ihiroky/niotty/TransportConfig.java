package net.ihiroky.niotty;

import java.util.Objects;

/**
 * Created on 13/01/15, 18:27
 *
 * @author Hiroki Itoh
 */
public class TransportConfig {

    private volatile PipelineComposer pipelineComposer_ = EMPTY;

    private static final PipelineComposer EMPTY = new EmptyPipelineComposer();

    public PipelineComposer getPipelineInitializer() {
        return pipelineComposer_;
    }

    public void setPipelineInitializer(PipelineComposer pipelineComposer) {
        Objects.requireNonNull(pipelineComposer, "pipelineComposer");
        this.pipelineComposer_ = pipelineComposer;
    }

    private static class EmptyPipelineComposer extends PipelineComposer {
        @Override
        public void compose(LoadPipeline loadPipeline, StorePipeline storePipeline) {
        }
    }
}
