package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.NameCountThreadFactory;
import net.ihiroky.niotty.PipelineComposer;
import net.ihiroky.niotty.Processor;

import java.util.Objects;

/**
 * @author Hiroki Itoh
 */
public class NioDatagramSocketProcessor
        implements Processor<NioDatagramSocketTransport, NioDatagramSocketConfig> {

    UdpIOSelectorPool ioSelectorPool_;
    private PipelineComposer pipelineComposer_;
    private String name_;
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
        pipelineComposer_ = PipelineComposer.empty();

        name_ = DEFAULT_NAME;
        numberOfMessageIOThread_ = DEFAULT_NUMBER_OF_MESSAGE_IO_THREAD;
        readBufferSize_ = DEFAULT_BUFFER_SIZE;
        writeBufferSize_ = DEFAULT_BUFFER_SIZE;
        useDirectBuffer_ = DEFAULT_DIRECT_BUFFER;
    }


    @Override
    public void start() {
        ioSelectorPool_.setReadBufferSize(readBufferSize_);
        ioSelectorPool_.setWriteBufferSize(writeBufferSize_);
        ioSelectorPool_.setDirect(useDirectBuffer_);
        ioSelectorPool_.open(new NameCountThreadFactory(name_.concat("-IO")), numberOfMessageIOThread_);
    }

    @Override
    public void stop() {
        ioSelectorPool_.close();
        pipelineComposer_.close();
    }

    @Override
    public String name() {
        return name_;
    }

    @Override
    public void setPipelineComposer(PipelineComposer composer) {
        Objects.requireNonNull(composer, "composer");
        pipelineComposer_ = composer;
    }

    @Override
    public NioDatagramSocketTransport createTransport(NioDatagramSocketConfig config) {
        return new NioDatagramSocketTransport(config, pipelineComposer_, name_, ioSelectorPool_);
    }

}
