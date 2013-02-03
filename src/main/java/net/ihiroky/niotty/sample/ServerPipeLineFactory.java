package net.ihiroky.niotty.sample;

import net.ihiroky.niotty.Niotty;
import net.ihiroky.niotty.PipeLine;
import net.ihiroky.niotty.PipeLineFactory;

/**
 * Created on 13/01/18, 18:48
 *
 * @author Hiroki Itoh
 */
public class ServerPipeLineFactory implements PipeLineFactory {
    @Override
    public PipeLine createLoadPipeLine() {
        return Niotty.newPipeLine("ServerLoadPipeLine", new StringDecoder(), new EchoStage());
    }

    @Override
    public PipeLine createStorePipeLine() {
        return Niotty.newPipeLine("ServerStorePipeLine", new StringEncoder());
    }
}
