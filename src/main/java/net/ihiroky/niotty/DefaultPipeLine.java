package net.ihiroky.niotty;

import net.ihiroky.niotty.event.MessageEvent;
import net.ihiroky.niotty.event.TransportStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * Created on 13/01/10, 17:21
 *
 * @author Hiroki Itoh
 */
public class DefaultPipeLine implements PipeLine {

    private String name;
    private StageContext<Object, Object> headContext;
    private StageContext<Object, Object> tailContext;
    private Logger logger = LoggerFactory.getLogger(DefaultPipeLine.class);

    protected DefaultPipeLine(String name) {
        this.name = String.valueOf(name);
        this.headContext = StageContext.TERMINAL;
        this.tailContext = StageContext.TERMINAL;
    }

    @Override
    public DefaultPipeLine add(Stage<?, ?> stage) {
        @SuppressWarnings("unchecked")
        Stage<Object, Object> s = (Stage<Object, Object>) stage;
        if (headContext == StageContext.TERMINAL) {
            headContext = tailContext = new StageContext<>(this, s);
            return this;
        }

        StageContext<Object, Object> context = new StageContext<>(this, s);
        tailContext.setNext(context);
        tailContext = context;
        return this;
    }

    public void verifyStageContextType() {
        if (logger.isDebugEnabled()) {
            int counter = 0;
            for (StageContext<?, ?> ctx = headContext; ctx != StageContext.TERMINAL; ctx = ctx.getNext()) {
                for (Type type : ctx.getStage().getClass().getGenericInterfaces()) {
                    if (type instanceof ParameterizedType) {
                        Type[] actualTypeArguments = ((ParameterizedType) type).getActualTypeArguments();
                        logger.debug("[verifyStageContextType] {}:{} - I:{}, O:{}",
                                name, counter++, actualTypeArguments[0], actualTypeArguments[1]);
                        break;
                    }
                }
            }
        }
    }

    @Override
    public StageContext<?, ?> getFirstContext() {
        return headContext;
    }

    @Override
    public StageContext<?, ?> getLastContext() {
        return tailContext;
    }

    @Override
    public StageContext<?, ?> searchContextFor(Class<? extends Stage<?, ?>> c) {
        for (StageContext<?, ?> context = headContext; context != tailContext; context = context.getNext()) {
            if (c.equals(context.getStage().getClass())) {
                return context;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public <S extends Stage<?, ?>> S searchStageFor(Class<? extends Stage<?, ?>> c) {
        return (S) searchContextFor(c).getStage();
    }

    @Override
    public void fire(MessageEvent<Object> event) {
        headContext.fire(event);
    }

    @Override
    public void fire(TransportStateEvent event) {
        headContext.fire(event);
    }
}
