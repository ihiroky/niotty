package net.ihiroky.niotty;

/**
 * Created on 13/01/09, 17:23
 *
 * @author Hiroki Itoh
 */
public interface StoreStage<I, O> {

    void store(StageContext<O> context, I input);
    void store(StageContext<O> context, TransportStateEvent event);
}
