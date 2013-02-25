package net.ihiroky.niotty;

import net.ihiroky.niotty.event.MessageEvent;
import net.ihiroky.niotty.event.TransportStateEvent;

import java.util.Objects;

/**
 * @author Hiroki Itoh
 */
public class StoreStageContext<I, O> extends StageContext<I, O> {

    private StoreStage<I, O> stage;

    @SuppressWarnings("unchecked")
    public StoreStageContext(Pipeline pipeline, StoreStage<Object, Object> stage) {
        super(pipeline);
        Objects.requireNonNull(stage,"stage");
        this.stage = (StoreStage<I, O>) stage;
    }

    @Override
    protected Object getStage() {
        return stage;
    }

    @Override
    protected void fire(MessageEvent<I> event) {
        callOnFire(event);
        stage.store(this, event);
    }

    @Override
    protected void fire(TransportStateEvent event) {
        callOnFire(event);
        stage.store(this, event);
    }


    @Override
    public String toString() {
        return "(store stage:" + stage + ')';
    }
}
