package net.ihiroky.niotty;

import net.ihiroky.niotty.event.MessageEvent;
import net.ihiroky.niotty.event.TransportStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created on 13/01/10, 17:21
 *
 * @author Hiroki Itoh
 */
public class DefaultPipeLine implements PipeLine {

    private StageContext headContext;
    private StageContext tailContext;
    private Logger logger = LoggerFactory.getLogger(DefaultPipeLine.class);

    private static final StageContext NULL = new StageContext(null, new Stage<Object>() {
        @Override
        public void process(StageContext context, MessageEvent<Object> event) {
        }
        @Override
        public void process(StageContext context, TransportStateEvent event) {
        }
        @Override
        public String toString() {
            return "null stage";
        }
    });

    protected DefaultPipeLine() {
        this.headContext = NULL;
        this.tailContext = NULL;
    }

    @Override
    public DefaultPipeLine add(Stage<?> stage) {
        if (headContext == NULL) {
            headContext = tailContext = new StageContext(this, stage);
            return this;
        }

        StageContext context = new StageContext(this, stage);
        tailContext.setNext(context);
        tailContext = context;
        return this;
    }

    @Override
    public StageContext getFirstContext() {
        return headContext;
    }

    @Override
    public StageContext getLastContext() {
        return tailContext;
    }

    @Override
    public StageContext searchContextFor(Class<? extends Stage<?>> c) {
        for (StageContext context = headContext; context != tailContext; context = context.getNext()) {
            if (c.equals(context.getStage().getClass())) {
                return context;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public <S extends Stage<?>> S searchStageFor(Class<? extends Stage<?>> c) {
        return (S) searchContextFor(c).getStage();
    }

    @Override
    public void fire(MessageEvent<?> event) {
        headContext.fire(event);
    }

    @Override
    public void fire(TransportStateEvent event) {
        headContext.fire(event);
    }
}
