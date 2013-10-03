package net.ihiroky.niotty;

import net.ihiroky.niotty.util.Arguments;

/**
 * @param <I> the type of the input object for the stage.
 * @param <O> the type of the output object for the stage.
 */
class LoadStageContext<I, O> extends PipelineElement<I, O> {

    private LoadStage<I, O> stage_;

    @SuppressWarnings("unchecked")
    LoadStageContext(AbstractPipeline<?, ?> pipeline,
                            StageKey key, LoadStage<Object, Object> stage, TaskLoopGroup<? extends TaskLoop> pool) {
        super(pipeline, key, pool);
        Arguments.requireNonNull(stage, "stage");
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
    protected void fire(I input, TransportParameter parameter) {
        StageContext<O> context = wrappedStageContext(this, parameter);
        stage_.load(context, input);
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
    public TransportParameter transportParameter() {
        return DefaultTransportParameter.NO_PARAMETER;
    }
}
