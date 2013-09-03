package net.ihiroky.niotty;

import java.util.Objects;

/**
 *
 */
public final class DefaultPipelineElementExecutorPool<L extends TaskLoop> implements PipelineElementExecutorPool {

    private final AbstractPipeline<?, L> pipeline_;
    private final TaskLoopGroup<? extends TaskLoop> taskLoopGroup_;

    DefaultPipelineElementExecutorPool(
            AbstractPipeline<?, L> pipeline, TaskLoopGroup<? extends TaskLoop> taskLoopGroup) {
        Objects.requireNonNull(pipeline, "pipeline");
        Objects.requireNonNull(taskLoopGroup, "taskLoopGroup");
        pipeline_ = pipeline;
        taskLoopGroup_ = taskLoopGroup;
    }

    @Override
    public TaskLoop assign(TaskSelection context) {
        // The task loop to be assigned is already assigned by I/O thread.
        // So there is no need to assign and reject.
        AbstractTransport<L> transport = pipeline_.transport();
        return taskLoopGroup_.assign(transport);
    }

    @Override
    public void close() {
    }
}
