package net.ihiroky.niotty;

import java.util.concurrent.TimeUnit;

/**
 *
 */
public interface StageContext {
    StageKey key();
    Transport transport();
    void proceed(Object message, Object parameter);
    EventFuture schedule(Event event, long timeout, TimeUnit timeUnit);
}
