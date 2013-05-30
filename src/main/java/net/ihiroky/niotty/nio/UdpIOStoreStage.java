package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.StageContext;
import net.ihiroky.niotty.TaskLoop;
import net.ihiroky.niotty.buffer.BufferSink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * @author Hiroki Itoh
 */
public class UdpIOStoreStage extends AbstractSelector.SelectorStoreStage<UdpIOSelector> {

    private ByteBuffer writeBuffer_;

    private Logger logger_ = LoggerFactory.getLogger(UdpIOStoreStage.class);

    UdpIOStoreStage(int writeBufferSize, boolean direct) {
        writeBuffer_ = direct ? ByteBuffer.allocateDirect(writeBufferSize) : ByteBuffer.allocate(writeBufferSize);
    }

    @Override
    public void store(StageContext<Void> context, BufferSink input) {
        final NioDatagramSocketTransport transport = (NioDatagramSocketTransport) context.transport();
        transport.readyToWrite(new AttachedMessage<>(input, context.transportParameter()));
        transport.offerTask(new TaskLoop.Task<UdpIOSelector>() {
            @Override
            public int execute(UdpIOSelector eventLoop) throws Exception {
                try {
                    return transport.flush(writeBuffer_);
                } catch (IOException ioe) {
                    logger_.error("failed to flush buffer to " + transport, ioe);
                    transport.closeSelectableChannel();
                }
                return 0;
            }
        });
    }
}
