package net.ihiroky.niotty.codec;

import net.ihiroky.niotty.LoadStage;
import net.ihiroky.niotty.StageContext;
import net.ihiroky.niotty.TransportStateEvent;
import net.ihiroky.niotty.buffer.CodecBuffer;

/**
 * @author Hiroki Itoh
 */
public class DelimiterRemoveDecoder implements LoadStage<CodecBuffer, CodecBuffer> {
    @Override
    public void load(StageContext<CodecBuffer> context, CodecBuffer input) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void load(StageContext<?> context, TransportStateEvent event) {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
