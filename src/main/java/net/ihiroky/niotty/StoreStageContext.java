package net.ihiroky.niotty;

import java.util.Objects;

/**
 * @author Hiroki Itoh
 */
public class StoreStageContext<I, O> extends PipelineElement<I, O> {

    private StoreStage<I, O> stage_;

    @SuppressWarnings("unchecked")
    public StoreStageContext(Pipeline<?> pipeline,
                             StageKey key, StoreStage<Object, Object> stage, PipelineElementExecutorPool pool) {
        super(pipeline, key, pool);
        Objects.requireNonNull(stage, "stage");
        this.stage_ = (StoreStage<I, O>) stage;
    }

    @Override
    protected StoreStage<I, O> stage() {
        return stage_;
    }

    @Override
    protected void fire(I input) {
        stage_.store(this, input);
    }

    @Override
    protected void fire(TransportStateEvent event) {
        stage_.store(this, event);
    }


    @Override
    public String toString() {
        return "(store stage:" + stage_ + ')';
    }
}
