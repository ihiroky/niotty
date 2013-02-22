package net.ihiroky.niotty;

import net.ihiroky.niotty.event.MessageEvent;
import net.ihiroky.niotty.event.TransportStateEvent;

/**
 * @author Hiroki Itoh
 */
public abstract class PairStage<LI, LO, SI, SO> {

    Stage<LI, LO> newLoadStage() {
        return new LoadStage();
    }

    Stage<SI, SO> newStoreStage() {
        return new StoreStage();
    }

    private class LoadStage implements Stage<LI, LO> {

        @Override
        public void process(StageContext<LI, LO> context, MessageEvent<LI> event) {
            load(context, event);
        }

        @Override
        public void process(StageContext<LI, LO> context, TransportStateEvent event) {
            load(context, event);
        }
    }

    private class StoreStage implements Stage<SI, SO> {

        @Override
        public void process(StageContext<SI, SO> context, MessageEvent<SI> event) {
            store(context, event);
        }

        @Override
        public void process(StageContext<SI, SO> context, TransportStateEvent event) {
            store(context, event);
        }
    }

    protected abstract void load(StageContext<LI, LO> context, MessageEvent<LI> event);
    protected abstract void load(StageContext<LI, LO> context, TransportStateEvent event);
    protected abstract void store(StageContext<SI, SO> context, MessageEvent<SI> event);
    protected abstract void store(StageContext<SI, SO> context, TransportStateEvent event);
}
