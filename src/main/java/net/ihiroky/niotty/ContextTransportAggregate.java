package net.ihiroky.niotty;

import net.ihiroky.niotty.buffer.BufferSink;
import net.ihiroky.niotty.event.MessageEvent;

import java.util.Objects;

/**
 * TODO proper name
 * @author Hiroki Itoh
 */
public class ContextTransportAggregate extends DefaultTransportAggregate {

    private PipeLine storePipeLine;

    ContextTransportAggregate(PipeLine storePipeLine) {
        Objects.requireNonNull(storePipeLine, "storePipeLine");
        storePipeLine.getLastContext().addListener(new StageContextAdapter<Object, BufferSink>() {
            @Override
            public void onProceed(
                    PipeLine pipeLine, StageContext<Object, BufferSink> context, MessageEvent<BufferSink> event) {
                final BufferSink buffer = event.getMessage();
                for (Transport t : transportMap.keySet()) {
                    @SuppressWarnings("unchecked")
                    AbstractTransport<?> transport = (AbstractTransport<?>) t;
                    transport.writeDirect(buffer);
                }
            }
        });
        this.storePipeLine = storePipeLine;
    }

    @Override
    public void write(Object message) {
        storePipeLine.fire(new MessageEvent<>(null, message));
    }
}
