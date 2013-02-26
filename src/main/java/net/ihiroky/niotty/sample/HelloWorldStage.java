package net.ihiroky.niotty.sample;

import net.ihiroky.niotty.LoadStage;
import net.ihiroky.niotty.LoadStageContext;
import net.ihiroky.niotty.event.MessageEvent;
import net.ihiroky.niotty.event.TransportStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Hiroki Itoh
 */
public class HelloWorldStage implements LoadStage<String, Object> {

    private Logger logger_ = LoggerFactory.getLogger(HelloWorldStage.class);

    @Override
    public void load(LoadStageContext<String, Object> context, MessageEvent<String> event) {
        logger_.info(event.toString());
        System.out.println(event.toString());
    }

    @Override
    public void load(LoadStageContext<String, Object> context, TransportStateEvent event) {
        switch (event.getState()) {
            case CONNECTED:
                if (event.getValue() != null) {
                    logger_.info(event.toString());
                    event.getTransport().write("Hello World.");
                }
                break;
        }
    }
}
