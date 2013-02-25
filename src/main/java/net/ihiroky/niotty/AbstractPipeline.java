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
public abstract class AbstractPipeline<S> implements Pipeline {

    private String name;
    private StageContext<Object, Object> headContext;
    private StageContext<Object, Object> tailContext;
    private Logger logger = LoggerFactory.getLogger(AbstractPipeline.class);

    private static final StageContext<Object, Object> TERMINAL = new NullContext();

    protected AbstractPipeline(String name) {
        this.name = name;
        this.headContext = TERMINAL;
        this.tailContext = TERMINAL;
    }

    protected void addStage(S stage) {
        if (headContext == TERMINAL) {
            StageContext<Object, Object> context = createContext(stage);
            context.setNext(TERMINAL);
            headContext = tailContext = context;
            return;
        }

        StageContext<Object, Object> context = createContext(stage);
        context.setNext(TERMINAL);
        tailContext.setNext(context);
        tailContext = context;
    }

    protected abstract StageContext<Object, Object> createContext(S stage);

    public void regulate() {
        if (headContext == TERMINAL) {
            StageContext<Object, Object> context = new NullContext();
            context.setNext(TERMINAL);
            headContext = tailContext = context;
        }
    }

    public void verifyStageContextType() {

        // TODO verify next input class is assignable from previous output class

        if (logger.isDebugEnabled()) {
            int counter = 0;
            for (StageContext<Object, Object> ctx = headContext; ctx != TERMINAL; ctx = ctx.getNext()) {
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

    public void fire(MessageEvent<Object> event) {
        headContext.fire(event);
    }

    public void fire(TransportStateEvent event) {
        headContext.fire(event);
    }

    public StageContext<Object, Object> getFirstContext() {
        return headContext;
    }

    public StageContext<Object, Object> getLastContext() {
        return tailContext;
    }

    public StageContext<Object, Object> searchContextFor(Class<?> stageClass) {
        for (StageContext<Object, Object> ctx = headContext; ctx != tailContext; ctx = ctx.getNext()) {
            if (stageClass.equals(ctx.getStage().getClass())) {
                return ctx;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public <S> S searchStageFor(Class<S> stageClass) {
        return stageClass.cast(searchContextFor(stageClass).getStage());
    }

    private static class NullContext extends StageContext<Object, Object> {
        protected NullContext() {
            super(null);
        }
        @Override
        protected Object getStage() {
            return this;
        }
        @Override
        protected void fire(MessageEvent<Object> event) {
        }
        @Override
        protected void fire(TransportStateEvent event) {
        }
    }
}
