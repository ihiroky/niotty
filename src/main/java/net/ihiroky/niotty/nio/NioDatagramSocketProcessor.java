package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.AbstractProcessor;
import net.ihiroky.niotty.NameCountThreadFactory;

/**
 * @author Hiroki Itoh
 */
public class NioDatagramSocketProcessor
        extends AbstractProcessor<NioDatagramSocketTransport, NioDatagramSocketConfig> {

    UdpIOSelectorPool ioSelectorPool_;
    private int numberOfMessageIOThread_;
    private int readBufferSize_;
    private int writeBufferSize_;
    private boolean useDirectBuffer_;

    private static final int DEFAULT_NUMBER_OF_MESSAGE_IO_THREAD =
            Math.max(Runtime.getRuntime().availableProcessors() / 2, 2);
    private static final int DEFAULT_BUFFER_SIZE = Short.MAX_VALUE;
    private static final boolean DEFAULT_DIRECT_BUFFER = true;

    private static final String DEFAULT_NAME = "NioDatagramSocket";

    public NioDatagramSocketProcessor() {
        ioSelectorPool_ = new UdpIOSelectorPool();

        numberOfMessageIOThread_ = DEFAULT_NUMBER_OF_MESSAGE_IO_THREAD;
        readBufferSize_ = DEFAULT_BUFFER_SIZE;
        writeBufferSize_ = DEFAULT_BUFFER_SIZE;
        useDirectBuffer_ = DEFAULT_DIRECT_BUFFER;
    }

    @Override
    protected void onStart() {
        ioSelectorPool_.setReadBufferSize(readBufferSize_);
        ioSelectorPool_.setWriteBufferSize(writeBufferSize_);
        ioSelectorPool_.setDirect(useDirectBuffer_);
        ioSelectorPool_.open(new NameCountThreadFactory(name().concat("-IO")), numberOfMessageIOThread_);

    }

    @Override
    protected void onStop() {
        ioSelectorPool_.close();
    }

    @Override
    public NioDatagramSocketTransport createTransport(NioDatagramSocketConfig config) {
        return new NioDatagramSocketTransport(config, pipelineComposer(), name(), ioSelectorPool_);
    }

}
