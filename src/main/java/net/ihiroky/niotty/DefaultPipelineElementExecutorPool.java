package net.ihiroky.niotty;

/**
 * @author Hiroki Itoh
 */
public final class DefaultPipelineElementExecutorPool implements PipelineElementExecutorPool {

    private static final DefaultPipelineElementExecutorPool INSTANCE = new DefaultPipelineElementExecutorPool();
    private static final DefaultPipelineElementExecutor EXECUTOR = new DefaultPipelineElementExecutor();

    private DefaultPipelineElementExecutorPool() {
    }

    public static DefaultPipelineElementExecutorPool instance() {
        return INSTANCE;
    }

    @Override
    public PipelineElementExecutor assign(PipelineElement<?, ?> context) {
        return EXECUTOR;
    }

    @Override
    public void close() {
    }
}
