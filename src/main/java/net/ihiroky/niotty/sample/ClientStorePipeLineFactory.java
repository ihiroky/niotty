package net.ihiroky.niotty.sample;

import net.ihiroky.niotty.PipeLine;
import net.ihiroky.niotty.PipeLineFactory;

/**
 * Created on 13/01/18, 16:48
 *
 * @author Hiroki Itoh
 */
public class ClientStorePipeLineFactory extends PipeLineFactory {
    @Override
    public PipeLine createPipeLine() {
        return newPipeLine(new StringEncoder());
    }
}
