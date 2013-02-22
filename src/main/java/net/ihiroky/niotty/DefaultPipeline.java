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
public class DefaultPipeline implements Pipeline {

    private String name;
    private StageContext<Object, Object> headContext;
    private StageContext<Object, Object> tailContext;
    private Logger logger = LoggerFactory.getLogger(DefaultPipeline.class);

    private static final String SUFFIX_LOAD = "[load]";
    private static final String UFFIX_STORE = "[store]";

    public static DefaultPipeline createLoadPipeLine(String name) {
        return new DefaultPipeline(String.valueOf(name).concat(SUFFIX_LOAD));
    }

    public static DefaultPipeline createStorePipeLine(String name) {
        return new DefaultPipeline(String.valueOf(name).concat(SUFFIX_LOAD));
    }

    protected DefaultPipeline(String name) {
        this.name = name;
        this.headContext = StageContext.TERMINAL;
        this.tailContext = StageContext.TERMINAL;
    }

    @Override
    public DefaultPipeline add(Stage<?, ?> stage) {
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

    public void regulate() {
        // requires at least one stage
        int count = 0;
        for (StageContext<?, ?> ctx = headContext; ctx != StageContext.TERMINAL; ctx = ctx.getNext()) {
            count++;
        }
        if (count == 0) {
            add(new EmptyStage());
        }
    }

    public void verifyStageContextType() {
        Class<?> outputClass;
        String outputClassName;
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

    private static class EmptyStage implements Stage<Object, Object> {

        @Override
        public void process(StageContext<Object, Object> context, MessageEvent<Object> event) {
        }

        @Override
        public void process(StageContext<Object, Object> context, TransportStateEvent event) {
        }
    }
}
