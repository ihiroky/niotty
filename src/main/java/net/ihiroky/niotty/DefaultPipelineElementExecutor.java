package net.ihiroky.niotty;

/**
 * @author Hiroki Itoh
 */
public class DefaultPipelineElementExecutor implements PipelineElementExecutor {
    @Override
    public <I> void execute(PipelineElement<I, ?> context, I input) {
        context.fire(input);
    }

    @Override
    public <I> void execute(PipelineElement<I, ?> context, AttachedMessage<I> input) {
        context.fire(input.message());
    }

    @Override
    public void execute(PipelineElement<?, ?> context, TransportStateEvent event) {
        context.fire(event);
    }

    @Override
    public PipelineElementExecutorPool pool() {
        return DefaultPipelineElementExecutorPool.instance();
    }

    @Override
    public void close(PipelineElement<?, ?> context) {
    }
}
