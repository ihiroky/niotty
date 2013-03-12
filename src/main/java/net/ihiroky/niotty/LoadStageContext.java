package net.ihiroky.niotty;

import java.util.Objects;

/**
 * @author Hiroki Itoh
 */
public class LoadStageContext<I, O> extends StageContext<I, O> {

    private LoadStage<I, O> stage_;

    @SuppressWarnings("unchecked")
    public LoadStageContext(Pipeline pipeline, LoadStage<Object, Object> stage, StageContextExecutor<I> executor) {
        super(pipeline, executor);
        Objects.requireNonNull(stage, "stage");
        this.stage_ = (LoadStage<I, O>) stage;
    }

    @Override
    protected LoadStage<I, O> getStage() {
        return stage_;
    }

    @Override
    protected void fire(I input) {
        callOnFire(input);
        stage_.load(this, input);
    }

    @Override
    protected void fire(TransportStateEvent event) {
        callOnFire(event);
        stage_.load(this, event);
    }

    @Override
    public String toString() {
        return "(load stage:" + stage_ + ')';
    }
}
