package net.ihiroky.niotty;

import java.util.concurrent.TimeUnit;

/**
 *
 */
public interface StageContext {
    StageKey key();
    Transport transport();
    Object parameter();
    void proceed(Object message);
    TaskFuture schedule(Task task, long timeout, TimeUnit timeUnit);
}
