package net.ihiroky.niotty;

import net.ihiroky.niotty.event.MessageEvent;
import net.ihiroky.niotty.event.TransportStateEvent;

/**
 * Created on 13/01/09, 17:21
 *
 * @author Hiroki Itoh
 */
public interface PipeLine {

    PipeLine add(Stage<?> stage);
    StageContext getFirstContext();
    StageContext getLastContext();
    StageContext searchContextFor(Class<? extends Stage<?>> c);
    <S extends Stage<?>> S searchStageFor(Class<? extends Stage<?>> c);
    void setContextTransportAggregate(ContextTransportAggregate contextTransportAggregate);
    void fire(MessageEvent<?> event);
    void fire(TransportStateEvent event);
}
