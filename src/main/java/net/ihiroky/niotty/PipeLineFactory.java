package net.ihiroky.niotty;

/**
 * Created on 13/01/09, 18:57
 *
 * @author Hiroki Itoh
 */
public interface PipeLineFactory {

    PipeLine createLoadPipeLine(TransportAggregate transportAggregate);
    PipeLine createStorePipeLine();
}
