package net.ihiroky.niotty.sample;

import net.ihiroky.niotty.PipeLine;
import net.ihiroky.niotty.PipeLineFactory;

/**
 * Created on 13/01/18, 18:48
 *
 * @author Hiroki Itoh
 */
public class ServerLoadPipeLineFactory extends PipeLineFactory {
    @Override
    public PipeLine createPipeLine() {
        return newPipeLine(new StringDecoder(), new EchoStage());
    }
}
