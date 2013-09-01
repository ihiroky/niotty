package net.ihiroky.niotty;

import java.util.Objects;

/**
 * @author Hiroki Itoh
 */
public final class DefaultPipelineElementExecutorPool<L extends TaskLoop> implements PipelineElementExecutorPool {

    private final AbstractPipeline<?, L> pipeline_;
    private final TaskLoopGroup<? extends TaskLoop> taskLoopGroup_;

    public DefaultPipelineElementExecutorPool(
            AbstractPipeline<?, L> pipeline, TaskLoopGroup<? extends TaskLoop> taskLoopGroup) {
        Objects.requireNonNull(pipeline, "pipeline");
        Objects.requireNonNull(taskLoopGroup, "taskLoopGroup");
        pipeline_ = pipeline;
        taskLoopGroup_ = taskLoopGroup;
    }

    public TaskLoopGroup<? extends TaskLoop> taskLoopGroup() {
        return taskLoopGroup_;
    }

    @Override
    public PipelineElementExecutor assign(PipelineElement<?, ?> context) {
        AbstractTransport<L> transport = pipeline_.transport();
        TaskLoop taskLoop = taskLoopGroup_.assign(transport);
        return new DefaultPipelineElementExecutor(taskLoop, this);
    }

    @Override
    public void close() {
    }
}
