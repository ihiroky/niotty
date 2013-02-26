package net.ihiroky.niotty.sample;

import net.ihiroky.niotty.LoadStage;
import net.ihiroky.niotty.LoadStageContext;
import net.ihiroky.niotty.event.MessageEvent;
import net.ihiroky.niotty.event.TransportStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created on 13/01/18, 16:56
 *
 * @author Hiroki Itoh
 */
public class EchoStage implements LoadStage<String, Object> {

    private Logger logger_ = LoggerFactory.getLogger(EchoStage.class);

    @Override
    public void load(LoadStageContext<String, Object> context, MessageEvent<String> event) {
        logger_.info(event.toString());
        event.getTransport().write(event.getMessage());
    }

    @Override
    public void load(LoadStageContext<String, Object> context, TransportStateEvent event) {
        logger_.info(event.toString());
    }
}
