package net.ihiroky.niotty.sample;

import net.ihiroky.niotty.PipeLine;
import net.ihiroky.niotty.PipeLineFactory;

/**
 * Created on 13/01/18, 18:49
 *
 * @author Hiroki Itoh
 */
public class ServerStorePipeLineFactory extends PipeLineFactory {
    @Override
    public PipeLine createPipeLine() {
        return newPipeLine(new StringEncoder());
    }
}
