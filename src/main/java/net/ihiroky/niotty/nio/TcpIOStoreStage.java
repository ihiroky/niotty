package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.StoreStageContext;
import net.ihiroky.niotty.TaskLoop;
import net.ihiroky.niotty.buffer.BufferSink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * @author Hiroki Itoh
 */
public class TcpIOStoreStage extends AbstractSelector.SelectorStoreStage<IOSelector> {

    static Logger logger_ = LoggerFactory.getLogger(TcpIOStoreStage.class);

    @Override
    public void store(StoreStageContext<BufferSink, Void> context, BufferSink input) {
        final NioClientSocketTransport transport = (NioClientSocketTransport) context.transport();
        transport.writeBufferSink(input);
        transport.offerTask(new TaskLoop.Task<IOSelector>() {
            @Override
            public int execute(IOSelector eventLoop) throws Exception {
                try {
                    return transport.flush();
                } catch (IOException ioe) {
                    logger_.error("failed to flush buffer to " + transport, ioe);
                    transport.closeSelectableChannel();
                }
                return 0;
            }
        });
    }
}
