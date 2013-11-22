package net.ihiroky.niotty;

/**
 *
 */
public abstract class StoreStage implements Stage {
    @Override
    public void loaded(StageContext context, Object message, Object parameter) {
        context.proceed(message, parameter);
    }
}
