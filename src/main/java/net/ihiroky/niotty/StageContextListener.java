package net.ihiroky.niotty;

/**
 * Created on 13/01/17, 12:59
 *
 * @author Hiroki Itoh
 */
public interface StageContextListener<I, O> {

    void onFire(Pipeline pipeline, StageContext<I, O> context, I input);
    void onFire(Pipeline pipeline, StageContext<I, O> context, TransportStateEvent event);
    void onProceed(Pipeline pipeline, StageContext<I, O> context, O output);
    void onProceed(Pipeline pipeline, StageContext<I, O> context, TransportStateEvent event);
}
