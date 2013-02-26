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

    private String name_;
    private StageContext<Object, Object> headContext_;
    private StageContext<Object, Object> tailContext_;
    private static Logger logger_ = LoggerFactory.getLogger(AbstractPipeline.class);

    private static final StageContext<Object, Object> TERMINAL = new NullContext();

    protected AbstractPipeline(String name) {
        this.name_ = name;
        this.headContext_ = TERMINAL;
        this.tailContext_ = TERMINAL;
    }

    protected void addStage(S stage) {
        if (headContext_ == TERMINAL) {
            StageContext<Object, Object> context = createContext(stage);
            context.setNext(TERMINAL);
            headContext_ = tailContext_ = context;
            return;
        }

        StageContext<Object, Object> context = createContext(stage);
        context.setNext(TERMINAL);
        tailContext_.setNext(context);
        tailContext_ = context;
    }

    protected abstract StageContext<Object, Object> createContext(S stage);

    public void regulate() {
        if (headContext_ == TERMINAL) {
            StageContext<Object, Object> context = new NullContext();
            context.setNext(TERMINAL);
            headContext_ = tailContext_ = context;
        }
    }

    public void verifyStageContextType() {

        // TODO verify next input class is assignable from previous output class

        if (logger_.isDebugEnabled()) {
            int counter = 0;
            for (StageContext<Object, Object> ctx = headContext_; ctx != TERMINAL; ctx = ctx.getNext()) {
                for (Type type : ctx.getStage().getClass().getGenericInterfaces()) {
                    if (type instanceof ParameterizedType) {
                        Type[] actualTypeArguments = ((ParameterizedType) type).getActualTypeArguments();
                        logger_.debug("[verifyStageContextType] {}:{} - I:{}, O:{}",
                                name_, counter++, actualTypeArguments[0], actualTypeArguments[1]);
                        break;
                    }
                }
            }
        }
    }

    public void fire(MessageEvent<Object> event) {
        headContext_.fire(event);
    }

    public void fire(TransportStateEvent event) {
        headContext_.fire(event);
    }

    public StageContext<Object, Object> getFirstContext() {
        return headContext_;
    }

    public StageContext<Object, Object> getLastContext() {
        return tailContext_;
    }

    public StageContext<Object, Object> searchContextFor(Class<?> stageClass) {
        for (StageContext<Object, Object> ctx = headContext_; ctx != tailContext_; ctx = ctx.getNext()) {
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
