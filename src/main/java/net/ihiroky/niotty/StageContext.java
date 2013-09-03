package net.ihiroky.niotty;

import java.util.concurrent.TimeUnit;

/**
 *
 */
public interface StageContext<O> {
    StageKey key();
    Transport transport();
    TransportParameter transportParameter();
    void proceed(O output);
    TaskFuture schedule(Task task, long timeout, TimeUnit timeUnit);
}
