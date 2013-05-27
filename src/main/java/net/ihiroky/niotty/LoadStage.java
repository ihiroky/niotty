package net.ihiroky.niotty;

/**
 * Created on 13/01/09, 17:23
 *
 * @author Hiroki Itoh
 */
public interface LoadStage<I, O> {

    void load(StageContext<O> context, I input);
    void load(StageContext<?> context, TransportStateEvent event);
}
