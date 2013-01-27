package net.ihiroky.niotty.sample;

import net.ihiroky.niotty.Niotty;
import net.ihiroky.niotty.PipeLine;
import net.ihiroky.niotty.PipeLineFactory;
import net.ihiroky.niotty.TransportAggregate;

/**
 * Created on 13/01/18, 18:48
 *
 * @author Hiroki Itoh
 */
public class ServerPipeLineFactory implements PipeLineFactory {
    @Override
    public PipeLine createLoadPipeLine(TransportAggregate transportAggregate) {
        return Niotty.newPipeLine(transportAggregate).add(new StringDecoder()).add(new EchoStage());
    }

    @Override
    public PipeLine createStorePipeLine() {
        return Niotty.newPipeLine().add(new StringEncoder());
    }
}
