package net.ihiroky.niotty;

/**
 * Created on 13/01/18, 15:52
 *
 * @author Hiroki Itoh
 */
public class StageContextAdapter<I, O> implements StageContextListener<I, O> {
    @Override
    public void onFire(Pipeline pipeline, StageContext<I, O> context, I input) {
    }

    @Override
    public void onFire(Pipeline pipeline, StageContext<I, O> context, TransportStateEvent event) {
    }

    @Override
    public void onProceed(Pipeline pipeline, StageContext<I, O> context, O output) {
    }

    @Override
    public void onProceed(Pipeline pipeline, StageContext<I, O> context, TransportStateEvent event) {
    }
}
