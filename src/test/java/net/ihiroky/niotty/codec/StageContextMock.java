package net.ihiroky.niotty.codec;

import net.ihiroky.niotty.StageContext;
import net.ihiroky.niotty.StageKey;
import net.ihiroky.niotty.StageKeys;
import net.ihiroky.niotty.Task;
import net.ihiroky.niotty.TaskFuture;
import net.ihiroky.niotty.Transport;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

/**
 * @author Hiroki Itoh
 */
public class StageContextMock<O> implements StageContext {

    Transport transport_;
    Object parameter_;
    Queue<O> proceededMessageEventQueue_;

    public StageContextMock() {
        this(null, new Object());
    }

    public StageContextMock(Transport transport, Object parameter) {
        transport_ = transport;
        parameter_ = parameter;
        proceededMessageEventQueue_ = new ArrayDeque<O>();
    }

    @Override
    public StageKey key() {
        return StageKeys.of("StageContextMock");
    }

    @Override
    public Transport transport() {
        return transport_;
    }

    @Override
    public Object parameter() {
        return parameter_;
    }

    @Override
    public void proceed(Object messageEvent) {
        @SuppressWarnings("unchecked")
        O output = (O) messageEvent;
        proceededMessageEventQueue_.add(output);
    }

    @Override
    public TaskFuture schedule(Task task, long timeout, TimeUnit timeUnit) {
        throw new UnsupportedOperationException();
    }

    public O pollEvent() {
        return proceededMessageEventQueue_.poll();
    }

    public boolean hasNoEvent() {
        return proceededMessageEventQueue_.isEmpty();
    }

    public int eventCount() {
        return proceededMessageEventQueue_.size();
    }
}
