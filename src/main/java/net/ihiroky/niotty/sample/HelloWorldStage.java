package net.ihiroky.niotty.sample;

import net.ihiroky.niotty.LoadStage;
import net.ihiroky.niotty.LoadStageContext;
import net.ihiroky.niotty.TransportStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Hiroki Itoh
 */
public class HelloWorldStage implements LoadStage<String, Object> {

    private Logger logger_ = LoggerFactory.getLogger(HelloWorldStage.class);

    @Override
    public void load(LoadStageContext<String, Object> context, String message) {
        logger_.info(message);
        System.out.println(message);
    }

    @Override
    public void load(LoadStageContext<String, Object> context, TransportStateEvent event) {
        switch (event.state()) {
            case CONNECTED:
                if (event.value() != null) {
                    logger_.info(event.toString());
                    context.transport().write("Hello World.");
                }
                break;
        }
    }
}
