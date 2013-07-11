package net.ihiroky.niotty;

/**
 * <p>A objects that executes {@link LoadStage} or {@link StoreStage} contained by a {@link PipelineElement}.</p>
 *
 * <p>Instances of this class is normally managed by {@link PipelineElementExecutorPool}. It can be reused
 * according to a strategy of the pool.</p>
 *
 * @author Hiroki Itoh
 */
public interface PipelineElementExecutor {

    /**
     * <p>Executes {@code LoadStage} or {@code StoreStage} in the specified {@code context}.</p>
     *
     * @param context the context that contains the stages.
     * @param input an input to be passed into the stages.
     * @param <I> the type of the input.
     */
    <I> void execute(PipelineElement<I, ?> context, I input);

    /**
     * <p>Executes {@code LoadStage} or {@code StoreStage} in the specified {@code context}
     * with the specified parameter.</p>
     *
     * @param context the context that contains the stages.
     * @param input an input to be passed into the stages.
     * @param parameter the parameter for I/O threads, which is accessible through the context.
     * @param <I> the type of the input.
     */
    <I> void execute(PipelineElement<I, ?> context, I input, TransportParameter parameter);

    /**
     * <p>Executes {@code LoadStage} or {@code StoreStage} in the specified {@code context}</p>
     *
     * @param context the context that contains the stages.
     * @param event a transport state that has a transport operation or result.
     */
    void execute(PipelineElement<?, ?> context, TransportStateEvent event);

    /**
     * <p>Returns the pool which manages this object.</p>
     * @return the pool which manages this object.
     */
    PipelineElementExecutorPool pool();

    /**
     * <p>Closes this instance and releases any used resources.</p>
     * @param context the context that contains the stages.
     */
    void close(PipelineElement<?, ?> context);
}
