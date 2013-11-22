package net.ihiroky.niotty;

import java.util.concurrent.TimeUnit;

/**
 *
 */
public interface StageContext {
    StageKey key();
    Transport transport();
    void proceed(Object message, Object parameter);
    TaskFuture schedule(Task task, long timeout, TimeUnit timeUnit);
}
