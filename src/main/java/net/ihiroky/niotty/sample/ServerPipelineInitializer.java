package net.ihiroky.niotty.sample;

import net.ihiroky.niotty.LoadPipeline;
import net.ihiroky.niotty.PipelineInitializer;
import net.ihiroky.niotty.StorePipeline;
import net.ihiroky.niotty.stage.codec.frame.LengthPrependEncoder;
import net.ihiroky.niotty.stage.codec.frame.LengthRemoveDecoder;

/**
 * Created on 13/01/18, 18:48
 *
 * @author Hiroki Itoh
 */
public class ServerPipelineInitializer implements PipelineInitializer {
    @Override
    public void setUpPipeline(LoadPipeline loadPipeline, StorePipeline storePipeline) {
        loadPipeline.add(new LengthRemoveDecoder()).add(new StringDecoder()).add(new EchoStage());
        storePipeline.add(new StringEncoder()).add(new LengthPrependEncoder());
    }
}
