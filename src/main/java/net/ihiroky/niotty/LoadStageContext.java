package net.ihiroky.niotty;

import java.util.Objects;

/**
 * @author Hiroki Itoh
 */
public class LoadStageContext<I, O> extends PipelineElement<I, O> {

    private LoadStage<I, O> stage_;

    @SuppressWarnings("unchecked")
    public LoadStageContext(Pipeline<?> pipeline,
                            StageKey key, LoadStage<Object, Object> stage, PipelineElementExecutorPool pool) {
        super(pipeline, key, pool);
        Objects.requireNonNull(stage, "stage");
        this.stage_ = (LoadStage<I, O>) stage;
    }

    @Override
    protected LoadStage<I, O> stage() {
        return stage_;
    }

    @Override
    protected void fire(I input) {
        stage_.load(this, input);
    }

    @Override
    protected void fire(AttachedMessage<I> input) {
        StageContext<O> context = input.wrappedContext(this);
        stage_.load(context, input.message());
    }

    @Override
    protected void fire(TransportStateEvent event) {
        stage_.load(this, event);
    }

    @Override
    public String toString() {
        return "(load stage:" + stage_ + ')';
    }

    @Override
    public Object attachment() {
        return null;
    }
}
