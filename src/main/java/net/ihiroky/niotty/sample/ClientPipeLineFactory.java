package net.ihiroky.niotty.sample;

import net.ihiroky.niotty.Niotty;
import net.ihiroky.niotty.PipeLine;
import net.ihiroky.niotty.PipeLineFactory;

/**
 * Created on 13/01/18, 16:53
 *
 * @author Hiroki Itoh
 */
public class ClientPipeLineFactory implements PipeLineFactory {
    @Override
    public PipeLine createLoadPipeLine() {
        return Niotty.newPipeLine("ClientLoadPipeLine", new StringDecoder(), new HelloWorldStage());
    }

    @Override
    public PipeLine createStorePipeLine() {
        return Niotty.newPipeLine("ClientStorePipeLine", new StringEncoder());
    }
}
