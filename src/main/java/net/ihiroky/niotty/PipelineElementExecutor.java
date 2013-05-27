package net.ihiroky.niotty;

/**
 * @author Hiroki Itoh
 */
public interface PipelineElementExecutor {
    <I> void execute(PipelineElement<I, ?> context, I input);
    void execute(PipelineElement<?, ?> context, TransportStateEvent event);
    PipelineElementExecutorPool pool();
    void close(PipelineElement<?, ?> context);
}
