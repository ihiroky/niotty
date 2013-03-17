package net.ihiroky.niotty;

import java.util.Objects;

/**
 * Created on 13/01/09, 17:24
 *
 * @author Hiroki Itoh
 */
public abstract class StageContext<I, O> {

    private final Pipeline<?> pipeline_;
    private final StageKey key_;
    private volatile StageContext<O, Object> next_;
    private final StageContextExecutor executor_;

    private static final StageContextExecutor DEFAULT_EXECUTOR = new DefaultStageContextExecutor();

    protected StageContext(Pipeline<?> pipeline, StageKey key, StageContextExecutorPool pool) {
        this.pipeline_ = pipeline;
        this.key_ = key;
        this.executor_ = (pool != null) ? pool.assign(this) : DEFAULT_EXECUTOR;
    }

    public StageKey key() {
        return key_;
    }

    protected Pipeline<?> pipeline() {
        return pipeline_;
    }

    protected StageContextExecutor executor() {
        return executor_;
    }

    protected StageContext<O, Object> next() {
        return next_;
    }

    protected void setNext(StageContext<O, Object> next) {
        Objects.requireNonNull(next, "next");
        this.next_ = next;
    }

    public Transport transport() {
        return pipeline_.transport();
    }

    protected void close() {
        executor_.close(this);
    }

    protected void execute(I input) {
        executor_.execute(this, input);
    }

    protected void execute(TransportStateEvent event) {
        executor_.execute(this, event);
    }

    public void proceed(O output) {
        next_.execute(output);
    }

    public void proceed(TransportStateEvent event) {
        next_.execute(event);
    }

    protected abstract Object stage();
    protected abstract void fire(I input);
    protected abstract void fire(TransportStateEvent event);
}
