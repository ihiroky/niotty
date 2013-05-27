package net.ihiroky.niotty;

import java.util.Objects;

/**
 * Created on 13/01/09, 17:24
 *
 * @author Hiroki Itoh
 */
public abstract class PipelineElement<I, O> implements StageContext<O> {

    private final Pipeline<?> pipeline_;
    private final StageKey key_;
    private volatile PipelineElement<O, Object> next_;
    private final PipelineElementExecutor executor_;

    // TODO add address field to hold DatagramChannel#send()/receive()
    // TODO add getter for address

    private static final PipelineElementExecutor DEFAULT_EXECUTOR = new DefaultPipelineElementExecutor();

    protected PipelineElement(Pipeline<?> pipeline, StageKey key, PipelineElementExecutorPool pool) {
        this.pipeline_ = pipeline;
        this.key_ = key;
        this.executor_ = (pool != null) ? pool.assign(this) : DEFAULT_EXECUTOR;
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

    protected void execute(I input) {
        executor_.execute(this, input);
    }

    protected void execute(AttachedMessage<I> input) {
        executor_.execute(this, input);
    }
    protected void execute(TransportStateEvent event) {
        executor_.execute(this, event);
    }

    @Override
    public void proceed(O output) {
        next_.execute(output);
    }

    protected void proceed(AttachedMessage<O> output) {
        next_.execute(output);
    }

    @Override
    public void proceed(TransportStateEvent event) {
        next_.execute(event);
    }

    protected abstract Object stage();
    protected abstract void fire(I input);
    protected abstract void fire(AttachedMessage<I> input);
    protected abstract void fire(TransportStateEvent event);
}
