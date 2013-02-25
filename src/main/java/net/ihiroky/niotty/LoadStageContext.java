package net.ihiroky.niotty;

import net.ihiroky.niotty.event.MessageEvent;
import net.ihiroky.niotty.event.TransportStateEvent;

import java.util.Objects;

/**
 * @author Hiroki Itoh
 */
public class LoadStageContext<I, O> extends StageContext<I, O> {

    private LoadStage<I, O> stage;

    @SuppressWarnings("unchecked")
    public LoadStageContext(Pipeline pipeline, LoadStage<Object, Object> stage) {
        super(pipeline);
        Objects.requireNonNull(stage, "stage");
        this.stage = (LoadStage<I, O>) stage;
    }

    @Override
    protected LoadStage<I, O> getStage() {
        return stage;
    }

    @Override
    protected void fire(MessageEvent<I> event) {
        callOnFire(event);
        stage.load(this, event);
    }

    @Override
    protected void fire(TransportStateEvent event) {
        callOnFire(event);
        stage.load(this, event);
    }

    @Override
    public String toString() {
        return "(load stage:" + stage + ')';
    }
}
