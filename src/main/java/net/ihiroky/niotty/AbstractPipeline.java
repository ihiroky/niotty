package net.ihiroky.niotty;

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
    private Transport transport_;
    private StageContext<Object, Object> headContext_;
    private StageContext<Object, Object> tailContext_;
    private static Logger logger_ = LoggerFactory.getLogger(AbstractPipeline.class);

    private static final StageContext<Object, Object> TERMINAL = new NullContext();

    protected AbstractPipeline(String name, Transport transport) {
        this.name_ = name;
        this.transport_ = transport;
        this.headContext_ = TERMINAL;
        this.tailContext_ = TERMINAL;
    }

    protected void addStage(S stage) {
        addStage(stage, null);
    }

    protected void addStage(S stage, StageContextExecutor<Object> executor) {
        if (headContext_ == TERMINAL) {
            StageContext<Object, Object> context = createContext(stage, executor);
            context.setNext(TERMINAL);
            headContext_ = tailContext_ = context;
            return;
        }

        StageContext<Object, Object> context = createContext(stage, executor);
        context.setNext(TERMINAL);
        tailContext_.setNext(context);
        tailContext_ = context;
    }

    protected abstract StageContext<Object, Object> createContext(S stage, StageContextExecutor<Object> executor);

    public void close() {
        for (StageContext<Object, Object> ctx = headContext_; ctx != TERMINAL; ctx = ctx.next()) {
            ctx.close();
        }
    }

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
            for (StageContext<Object, Object> ctx = headContext_; ctx != TERMINAL; ctx = ctx.next()) {
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

    public void execute(Object input) {
        headContext_.execute(input);
    }

    public void execute(TransportStateEvent event) {
        headContext_.execute(event);
    }

    @Override
    public Transport transport() {
        return transport_;
    }

    @Override
    public StageContext<Object, Object> getFirstContext() {
        return headContext_;
    }

    @Override
    public StageContext<Object, Object> getLastContext() {
        return tailContext_;
    }

    @Override
    public StageContext<Object, Object> searchContextFor(Class<?> stageClass) {
        for (StageContext<Object, Object> ctx = headContext_; ctx != tailContext_; ctx = ctx.next()) {
            if (stageClass.equals(ctx.getStage().getClass())) {
                return ctx;
            }
        }
        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <S> S searchStageFor(Class<S> stageClass) {
        return stageClass.cast(searchContextFor(stageClass).getStage());
    }

    private static class NullContext extends StageContext<Object, Object> {
        protected NullContext() {
            super(null, null);
        }
        @Override
        protected Object getStage() {
            return this;
        }
        @Override
        protected void fire(Object input) {
        }
        @Override
        protected void fire(TransportStateEvent event) {
        }
    }
}
