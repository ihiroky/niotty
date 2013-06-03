package net.ihiroky.niotty;

/**
 * <p>An object that assigns {@link PipelineElementExecutor} from a pool.</p>
 *
 * <p>It is finished using, {@link #close()} must be called to release used resources.</p>
 *
 * @author Hiroki Itoh
 */
public interface PipelineElementExecutorPool extends AutoCloseable {

    /**
     * <p>Assigns {@link PipelineElementExecutor} for the given {@code context}.</p>
     *
     * <p>A {@link LoadStage} or {@link StoreStage} contained in the context
     * is executed by {@code PipelineElementExecutor}.</p>
     *
     * @param context the context.
     * @return a {@code PipelineElementExecutor}.
     */
    PipelineElementExecutor assign(PipelineElement<?, ?> context);

    /**
     * <p>Closes this pool and release any used resources.</p>
     */
    void close();
}
