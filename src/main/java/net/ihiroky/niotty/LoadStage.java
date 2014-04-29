package net.ihiroky.niotty;

/**
 *
 */
public abstract class LoadStage implements Stage {
    @Override
    public final void stored(StageContext context, Object message, Object parameter) {
        context.proceed(message, parameter);
    }
}
