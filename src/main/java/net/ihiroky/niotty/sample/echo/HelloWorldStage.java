package net.ihiroky.niotty.sample.echo;

import net.ihiroky.niotty.DeactivateState;
import net.ihiroky.niotty.LoadStage;
import net.ihiroky.niotty.StageContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Hiroki Itoh
 */
public class HelloWorldStage extends LoadStage {

    private Logger logger_ = LoggerFactory.getLogger(HelloWorldStage.class);

    @Override
    public void loaded(StageContext context, Object message, Object parameter) {
        String input = (String) message;
        logger_.info(input);
        System.out.println(input + " (from " + this.getClass().getName() + ")");
    }

    @Override
    public void exceptionCaught(StageContext context, Exception exception) {
        logger_.error("[exceptionCaught]", exception);
    }

    @Override
    public void activated(StageContext context) {
        logger_.info("[activated]");
    }

    @Override
    public void deactivated(StageContext context, DeactivateState state) {
        logger_.info("[deactivate] state:{}", state);
    }
}
