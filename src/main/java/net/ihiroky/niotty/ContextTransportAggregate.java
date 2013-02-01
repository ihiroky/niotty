package net.ihiroky.niotty;

import net.ihiroky.niotty.event.MessageEvent;

import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * TODO proper name
 * @author Hiroki Itoh
 */
public class ContextTransportAggregate extends DefaultTransportAggregate {

    private PipeLine storePipeLine;

    ContextTransportAggregate(PipeLine storePipeLine) {
        Objects.requireNonNull(storePipeLine, "storePipeLine");
        storePipeLine.getLastContext().addListener(new StageContextAdapter<ByteBuffer>() {
            @Override
            public void onProceed(PipeLine pipeLine, StageContext context, MessageEvent<ByteBuffer> event) {
                final ByteBuffer byteBuffer = event.getMessage();
                for (Transport t : transportMap.keySet()) {
                    @SuppressWarnings("unchecked")
                    AbstractTransport<?> transport = (AbstractTransport<?>) t;
                    transport.writeDirect(byteBuffer);
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
