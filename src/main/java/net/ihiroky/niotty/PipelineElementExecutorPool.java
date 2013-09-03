package net.ihiroky.niotty;

/**
 * <p>An object that assigns {@link TaskLoop} from a pool.</p>
 * <p>The algorithm to allocate {@code PipelineElementExecutor} is based on
 * {@link net.ihiroky.niotty.TaskLoopGroup}.</p>
 *
 * <p>It is finished using, {@link #close()} must be called to release used resources.</p>
 *
 * @author Hiroki Itoh
 */
public interface PipelineElementExecutorPool extends AutoCloseable {

    /**
     * <p>Assigns {@link TaskLoop} for the given {@code context}.</p>
     *
     * <p>A {@link LoadStage} or {@link StoreStage} contained in the context
     * is executed by {@code PipelineElementExecutor}.</p>
     *
     * @param context the context.
     * @return a {@code PipelineElementExecutor}.
     */
    TaskLoop assign(TaskSelection context);

    /**
     * <p>Closes this pool and release any used resources.</p>
     */
    void close();
}
