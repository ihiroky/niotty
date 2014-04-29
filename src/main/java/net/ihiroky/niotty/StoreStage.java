package net.ihiroky.niotty;

/**
 *
 */
public abstract class StoreStage implements Stage {
    @Override
    public final void loaded(StageContext context, Object message, Object parameter) {
        context.proceed(message, parameter);
    }
}
