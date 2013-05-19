package net.ihiroky.niotty.sample.echo;

import net.ihiroky.niotty.LoadStage;
import net.ihiroky.niotty.LoadStageContext;
import net.ihiroky.niotty.TransportStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created on 13/01/18, 16:56
 *
 * @author Hiroki Itoh
 */
public class EchoStage implements LoadStage<String, Void> {

    private Logger logger_ = LoggerFactory.getLogger(EchoStage.class);

    @Override
    public void load(LoadStageContext<String, Void> context, String message) {
        logger_.info(message);
        System.out.println(message + " (from " + this.getClass().getName() + ")");
        context.transport().write(message);
    }

    @Override
    public void load(LoadStageContext<?, ?> context, TransportStateEvent event) {
        logger_.info("state: {}", event.toString());
    }
}
