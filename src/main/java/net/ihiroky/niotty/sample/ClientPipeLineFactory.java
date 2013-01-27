package net.ihiroky.niotty.sample;

import net.ihiroky.niotty.Niotty;
import net.ihiroky.niotty.PipeLine;
import net.ihiroky.niotty.PipeLineFactory;
import net.ihiroky.niotty.TransportAggregate;

/**
 * Created on 13/01/18, 16:53
 *
 * @author Hiroki Itoh
 */
public class ClientPipeLineFactory implements PipeLineFactory {
    @Override
    public PipeLine createLoadPipeLine(TransportAggregate transportAggregate) {
        return Niotty.newPipeLine(transportAggregate).add(new StringDecoder()).add(new HelloWorldStage());
    }

    @Override
    public PipeLine createStorePipeLine() {
        return Niotty.newPipeLine().add(new StringEncoder());
    }
}
