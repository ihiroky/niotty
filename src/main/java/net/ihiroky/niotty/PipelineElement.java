package net.ihiroky.niotty;

import net.ihiroky.niotty.util.Arguments;

import java.util.concurrent.TimeUnit;

/**
 * @param <I> the type of the input object for the stage.
 * @param <O> the type of the output object for the stage.
 */
public abstract class PipelineElement<I, O> implements StageContext<O> {

    private final AbstractPipeline<?, ?> pipeline_;
    private final StageKey key_;
    private volatile PipelineElement<O, Object> next_;
    private final TaskLoop taskLoop_;

    protected PipelineElement(AbstractPipeline<?, ?> pipeline, StageKey key, TaskLoopGroup<? extends TaskLoop> pool) {
        pipeline_ = Arguments.requireNonNull(pipeline, "pipeline");
        key_ = Arguments.requireNonNull(key, "key");
        taskLoop_ = Arguments.requireNonNull(pool, "pool").assign(pipeline.transport());
    }

    PipelineElement(TaskLoop taskLoop) {
        pipeline_ = null;
        key_ = null;
        taskLoop_ = taskLoop;
    }

    @Override
    public StageKey key() {
        return key_;
    }

    protected TaskLoop taskLoop() {
        return taskLoop_;
    }

    protected Pipeline<?> pipeline() {
        return pipeline_;
    }

    protected PipelineElement<O, Object> next() {
        return next_;
    }

    protected void setNext(PipelineElement<O, Object> next) {
        Arguments.requireNonNull(next, "next");
        this.next_ = next;
    }

    @Override
    public Transport transport() {
        return pipeline_.transport();
    }

    protected void close() {
        taskLoop_.reject(pipeline_.transport());
    }

    @Override
    public void proceed(final O output) {
        TaskLoop tl = next_.taskLoop();
        if (tl.isInLoopThread()) {
            next_.fire(output);
        } else {
            tl.offer(new Task() {
                @Override
                public long execute(TimeUnit timeUnit) throws Exception {
                    next_.fire(output);
                    return DONE;
                }
            });
        }
    }

    @Override
    public TaskFuture schedule(Task task, long timeout, TimeUnit timeUnit) {
        return taskLoop_.schedule(task, timeout, timeUnit);
    }

    protected void proceed(final O output, final TransportParameter parameter) {
        TaskLoop tl = next_.taskLoop_;
        if (tl.isInLoopThread()) {
            next_.fire(output, parameter);
        } else {
            tl.offer(new Task() {
                @Override
                public long execute(TimeUnit timeUnit) throws Exception {
                    next_.fire(output, parameter);
                    return DONE;
                }
            });
        }
    }

    protected void proceed(final TransportStateEvent event) {
        TaskLoop tl = next_.taskLoop_;
        if (tl.isInLoopThread()) {
            next_.fire(event);
            next_.proceed(event);
        } else {
            tl.offer(new Task() {
                @Override
                public long execute(TimeUnit timeUnit) throws Exception {
                    next_.fire(event);
                    next_.proceed(event);
                    return DONE;
                }
            });
        }
    }

    protected StageContext<O> wrappedStageContext(PipelineElement<?, O> context, TransportParameter parameter) {
        return new WrappedStageContext<O>(context, parameter);
    }

    static class WrappedStageContext<O> implements StageContext<O> {

        private final PipelineElement<?, O> context_;
        private final TransportParameter parameter_;

        WrappedStageContext(PipelineElement<?, O> context, TransportParameter parameter) {
            context_ = context;
            parameter_ = parameter;
        }

        @Override
        public StageKey key() {
            return context_.key();
        }

        @Override
        public Transport transport() {
            return context_.transport();
        }

        @Override
        public TransportParameter transportParameter() {
            return parameter_;
        }

        @Override
        public void proceed(O output) {
            context_.proceed(output, parameter_);
        }

        @Override
        public TaskFuture schedule(Task task, long timeout, TimeUnit timeUnit) {
            return context_.schedule(task, timeout, timeUnit);
        }
    }

    protected abstract Object stage();
    protected abstract void fire(I input);
    protected abstract void fire(I input, TransportParameter parameter);
    protected abstract void fire(TransportStateEvent event);
}
