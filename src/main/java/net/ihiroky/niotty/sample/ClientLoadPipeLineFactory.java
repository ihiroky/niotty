package net.ihiroky.niotty.sample;

import net.ihiroky.niotty.PipeLine;
import net.ihiroky.niotty.PipeLineFactory;

/**
 * Created on 13/01/18, 16:53
 *
 * @author Hiroki Itoh
 */
public class ClientLoadPipeLineFactory extends PipeLineFactory {
    @Override
    public PipeLine createPipeLine() {
        return newPipeLine(new StringDecoder(), new HelloWorldStage());
    }
}
