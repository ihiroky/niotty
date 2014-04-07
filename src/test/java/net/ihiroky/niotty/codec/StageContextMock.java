package net.ihiroky.niotty.codec;

import net.ihiroky.niotty.Event;
import net.ihiroky.niotty.EventFuture;
import net.ihiroky.niotty.StageContext;
import net.ihiroky.niotty.StageKey;
import net.ihiroky.niotty.StageKeys;
import net.ihiroky.niotty.Transport;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

/**
 *
 */
public class StageContextMock<O> implements StageContext {

    Transport transport_;
    Queue<O> proceededMessageEventQueue_;
    List<Object> proceededParameterList_;
    boolean changesDispatcherOnProceed_;

    public StageContextMock() {
        this(null, false);
    }

    public StageContextMock(Transport transport, boolean changesDispatcherOnProceed) {
        transport_ = transport;
        proceededMessageEventQueue_ = new ArrayDeque<O>();
        proceededParameterList_ = new ArrayList<Object>();
        changesDispatcherOnProceed_ = changesDispatcherOnProceed;
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
    public void proceed(Object messageEvent, Object parameter) {
        @SuppressWarnings("unchecked")
        O output = (O) messageEvent;
        proceededMessageEventQueue_.add(output);
        proceededParameterList_.add(parameter);
    }

    @Override
    public EventFuture schedule(Event event, long timeout, TimeUnit timeUnit) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean changesDispatcherOnProceed() {
        return changesDispatcherOnProceed_;
    }

    public O pollEvent() {
        return proceededMessageEventQueue_.poll();
    }

    public boolean hasNoEvent() {
        return proceededMessageEventQueue_.isEmpty();
    }

    public List<Object> parameters() {
        return proceededParameterList_;
    }

    public int eventCount() {
        return proceededMessageEventQueue_.size();
    }

    public int parameterCount() {
        return proceededParameterList_.size();
    }
}
