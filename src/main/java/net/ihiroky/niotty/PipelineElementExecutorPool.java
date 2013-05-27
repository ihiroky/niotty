package net.ihiroky.niotty;

/**
 * @author Hiroki Itoh
 */
public interface PipelineElementExecutorPool extends AutoCloseable {
    PipelineElementExecutor assign(PipelineElement<?, ?> context);
    void close();
}
