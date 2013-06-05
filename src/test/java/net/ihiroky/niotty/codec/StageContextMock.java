package net.ihiroky.niotty.codec;

import net.ihiroky.niotty.StageContext;
import net.ihiroky.niotty.StageKey;
import net.ihiroky.niotty.StageKeys;
import net.ihiroky.niotty.Transport;
import net.ihiroky.niotty.TransportParameter;

import java.util.ArrayDeque;
import java.util.Queue;

/**
 * @author Hiroki Itoh
 */
public class StageContextMock<O> implements StageContext<O> {

    Transport transport_;
    TransportParameter transportParameter_;
    Queue<O> proceededMessageEventQueue_;

    public StageContextMock() {
        this(null, null);
    }

    public StageContextMock(Transport transport, TransportParameter transportParameter) {
        transport_ = transport;
        transportParameter_ = transportParameter;
        proceededMessageEventQueue_ = new ArrayDeque<>();
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
    public TransportParameter transportParameter() {
        return transportParameter_;
    }

    @Override
    public void proceed(O messageEvent) {
        proceededMessageEventQueue_.add(messageEvent);
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
