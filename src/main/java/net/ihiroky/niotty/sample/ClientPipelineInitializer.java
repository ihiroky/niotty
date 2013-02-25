package net.ihiroky.niotty.sample;

import net.ihiroky.niotty.LoadPipeline;
import net.ihiroky.niotty.PipelineInitializer;
import net.ihiroky.niotty.StorePipeline;
import net.ihiroky.niotty.stage.codec.frame.LengthPrependEncoder;
import net.ihiroky.niotty.stage.codec.frame.LengthRemoveDecoder;

/**
 * Created on 13/01/18, 16:53
 *
 * @author Hiroki Itoh
 */
public class ClientPipelineInitializer implements PipelineInitializer {
    @Override
    public void setUpPipeline(LoadPipeline loadPipeline, StorePipeline storePipeline) {
        loadPipeline.add(new LengthRemoveDecoder()).add(new StringDecoder()).add(new HelloWorldStage());
        storePipeline.add(new StringEncoder()).add(new LengthPrependEncoder());
    }
}
