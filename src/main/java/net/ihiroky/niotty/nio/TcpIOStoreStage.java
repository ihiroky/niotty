package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.AttachedMessage;
import net.ihiroky.niotty.StageContext;
import net.ihiroky.niotty.TaskLoop;
import net.ihiroky.niotty.TransportState;
import net.ihiroky.niotty.buffer.BufferSink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * @author Hiroki Itoh
 */
public class TcpIOStoreStage extends AbstractSelector.SelectorStoreStage<TcpIOSelector> {

    static Logger logger_ = LoggerFactory.getLogger(TcpIOStoreStage.class);

    @Override
    public void store(StageContext<Void> context, BufferSink input) {
        final NioClientSocketTransport transport = (NioClientSocketTransport) context.transport();
        transport.readyToWrite(new AttachedMessage<>(input, context.transportParameter()));
        transport.offerTask(new TaskLoop.Task<TcpIOSelector>() {
            @Override
            public int execute(TcpIOSelector eventLoop) throws Exception {
                try {
                    return transport.flush();
                } catch (IOException ioe) {
                    logger_.error("failed to flush buffer to " + transport, ioe);
                    transport.closeSelectableChannel(TransportState.CONNECTED);
                }
                return 0;
            }
        });
    }
}
