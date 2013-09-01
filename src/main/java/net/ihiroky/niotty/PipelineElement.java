package net.ihiroky.niotty;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Created on 13/01/09, 17:24
 *
 * @author Hiroki Itoh
 */
public abstract class PipelineElement<I, O> implements StageContext<O>, TaskSelection {

    private final Pipeline<?> pipeline_;
    private final StageKey key_;
    private volatile PipelineElement<O, Object> next_;
    private final PipelineElementExecutor executor_;

    protected <L extends TaskLoop> PipelineElement(
            Pipeline<?> pipeline, StageKey key, PipelineElementExecutorPool pool) {
        Objects.requireNonNull(pool, "pool");
        this.pipeline_ = pipeline;
        this.key_ = key;
        this.executor_ = pool.assign(this);
    }

    @Override
    public StageKey key() {
        return key_;
    }

    protected Pipeline<?> pipeline() {
        return pipeline_;
    }

    protected PipelineElementExecutor executor() {
        return executor_;
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
        executor_.close(this);
    }

    @Override
    public void proceed(O output) {
        next_.executor_.execute(next_, output);
    }

    @Override
    public TaskFuture schedule(Task task, long timeout, TimeUnit timeUnit) {
        return executor_.schedule(task, timeout, timeUnit);
    }

    protected void proceed(O output, TransportParameter parameter) {
        next_.executor_.execute(next_, output, parameter);
    }

    protected void proceed(TransportStateEvent event) {
        next_.executor_.execute(next_, event);
    }

    protected StageContext<O> wrappedStageContext(PipelineElement<?, O> context, TransportParameter parameter) {
        return new WrappedStageContext<>(context, parameter);
    }

    @Override
    public int weight() {
        return 1; // TODO correspond to transport ?
    }

    private static class WrappedStageContext<O> implements StageContext<O> {

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
