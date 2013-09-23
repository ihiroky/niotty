package net.ihiroky.niotty;

import net.ihiroky.niotty.util.Objects;

/**
 * @param <I> the type of the input object for the stage.
 * @param <O> the type of the output object for the stage.
 */
class StoreStageContext<I, O> extends PipelineElement<I, O> {

    private StoreStage<I, O> stage_;

    @SuppressWarnings("unchecked")
    StoreStageContext(AbstractPipeline<?, ?> pipeline,
                             StageKey key, StoreStage<Object, Object> stage, TaskLoopGroup<? extends TaskLoop> pool) {
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
    protected void fire(I input, TransportParameter parameter) {
        StageContext<O> context = wrappedStageContext(this, parameter);
        stage_.store(context, input);
    }

    @Override
    protected void fire(TransportStateEvent event) {
        stage_.store(this, event);
    }


    @Override
    public String toString() {
        return "(store stage:" + stage_ + ')';
    }

    @Override
    public TransportParameter transportParameter() {
        return DefaultTransportParameter.NO_PARAMETER;
    }
}
