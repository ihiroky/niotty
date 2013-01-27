package net.ihiroky.niotty.sample;

import net.ihiroky.niotty.Stage;
import net.ihiroky.niotty.StageContext;
import net.ihiroky.niotty.event.MessageEvent;
import net.ihiroky.niotty.event.TransportStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created on 13/01/18, 16:56
 *
 * @author Hiroki Itoh
 */
public class EchoStage implements Stage<String> {

    private Logger logger = LoggerFactory.getLogger(EchoStage.class);

    @Override
    public void process(StageContext context, MessageEvent<String> event) {
        logger.info(event.toString());
        event.getTransport().write(event.getMessage());
    }

    @Override
    public void process(StageContext context, TransportStateEvent event) {
        logger.info(event.toString());
        context.getContextTransportAggregate().add(event.getTransport());
    }
}
