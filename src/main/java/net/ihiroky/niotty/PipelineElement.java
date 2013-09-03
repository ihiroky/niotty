package net.ihiroky.niotty;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * @param <I> the type of the input object for the stage.
 * @param <O> the type of the output object for the stage.
 */
public abstract class PipelineElement<I, O> implements StageContext<O>, TaskSelection {

    private final AbstractPipeline<?, ?> pipeline_;
    private final StageKey key_;
    private volatile PipelineElement<O, Object> next_;
    private final TaskLoop taskLoop_;

    protected PipelineElement(AbstractPipeline<?, ?> pipeline, StageKey key, PipelineElementExecutorPool pool) {
        Objects.requireNonNull(pool, "pool");
        pipeline_ = pipeline;
        key_ = key;
        taskLoop_ = pool.assign(this);
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
        Objects.requireNonNull(next, "next");
        this.next_ = next;
    }

    @Override
    public Transport transport() {
        return pipeline_.transport();
    }

    protected void close() {
        taskLoop_.reject(this);
    }

    @Override
    public void proceed(final O output) {
        next_.taskLoop_.execute(new Task() {
            @Override
            public long execute(TimeUnit timeUnit) throws Exception {
                next_.fire(output);
                return DONE;
            }
        });
    }

    @Override
    public TaskFuture schedule(Task task, long timeout, TimeUnit timeUnit) {
        return taskLoop_.schedule(task, timeout, timeUnit);
    }

    protected void proceed(final O output, final TransportParameter parameter) {
        next_.taskLoop_.execute(new Task() {
            @Override
            public long execute(TimeUnit timeUnit) throws Exception {
                next_.fire(output, parameter);
                return DONE;
            }
        });
    }

    protected void proceed(final TransportStateEvent event) {
        next_.taskLoop_.execute(new Task() {
            @Override
            public long execute(TimeUnit timeUnit) throws Exception {
                next_.fire(event);
                next_.proceed(event);
                return DONE;
            }
        });
    }

    protected StageContext<O> wrappedStageContext(PipelineElement<?, O> context, TransportParameter parameter) {
        return new WrappedStageContext<>(context, parameter);
    }

    @Override
    public int weight() {
        return 1; // TODO correspond to transport ?
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
