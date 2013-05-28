package net.ihiroky.niotty;

/**
 * @author Hiroki Itoh
 */
public interface PipelineElementExecutor {
    <I> void execute(PipelineElement<I, ?> context, I input);
    <I> void execute(PipelineElement<I, ?> context, I input, TransportParameter parameter);
    void execute(PipelineElement<?, ?> context, TransportStateEvent event);
    PipelineElementExecutorPool pool();
    void close(PipelineElement<?, ?> context);
}
