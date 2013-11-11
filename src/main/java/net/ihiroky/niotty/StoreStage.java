package net.ihiroky.niotty;

/**
 *
 */
public abstract class StoreStage implements Stage {
    @Override
    public void loaded(StageContext context, Object message) {
        context.proceed(message);
    }
}
