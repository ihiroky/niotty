package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.AbstractProcessor;
import net.ihiroky.niotty.DefaultTaskTimer;
import net.ihiroky.niotty.NameCountThreadFactory;
import net.ihiroky.niotty.PipelineComposer;
import net.ihiroky.niotty.TaskTimer;

import java.net.ProtocolFamily;
import java.util.Objects;

/**
 * An implementation of {@link net.ihiroky.niotty.Processor} for NIO {@code DatagramChannel}.
 */
public class NioDatagramSocketProcessor extends AbstractProcessor<NioDatagramSocketTransport> {

    private UdpIOSelectorPool ioSelectorPool_;
    private int numberOfMessageIOThread_;
    private int readBufferSize_;
    private int writeBufferSize_;
    private boolean useDirectBuffer_;
    private WriteQueueFactory writeQueueFactory_;
    private TaskTimer taskTimer_;

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
        writeQueueFactory_ = new SimpleWriteQueueFactory();
        setName(DEFAULT_NAME);
    }

    @Override
    protected void onStart() {
        ioSelectorPool_.setReadBufferSize(readBufferSize_);
        ioSelectorPool_.setWriteBufferSize(writeBufferSize_);
        ioSelectorPool_.setDirect(useDirectBuffer_);
        if (taskTimer_ == null) {
            taskTimer_ = new DefaultTaskTimer(name());
        }
        ioSelectorPool_.setTaskTimer(taskTimer_);
        ioSelectorPool_.open(new NameCountThreadFactory(name().concat("-IO")), numberOfMessageIOThread_);
    }

    @Override
    protected void onStop() {
        ioSelectorPool_.close();
    }

    @Override
    public NioDatagramSocketTransport createTransport() {
        return new NioDatagramSocketTransport(
                pipelineComposer(), null, name(), ioSelectorPool_, writeQueueFactory_);
    }

    /**
     * Constructs the transport.
     * The {@code protocolFamily} parameter is used to specify the {@code ProtocolFamily}.
     * If the datagram channel is to be used for IP multicasing then this should correspond to
     * the address type of the multicast groups that this channel will join.
     *
     * @param protocolFamily the protocolFamily
     * @return the transport.
     */
    public NioDatagramSocketTransport createTransport(ProtocolFamily protocolFamily) {
        return new NioDatagramSocketTransport(
                pipelineComposer(), protocolFamily, name(), ioSelectorPool_, writeQueueFactory_);
    }

    @Override
    public NioDatagramSocketProcessor setName(String name) {
        super.setName(name);
        return this;
    }

    @Override
    public NioDatagramSocketProcessor setPipelineComposer(PipelineComposer composer) {
        super.setPipelineComposer(composer);
        return this;
    }

    public NioDatagramSocketProcessor setNumberOfMessageIOThread(int numberOfMessageIOThread) {
        if (numberOfMessageIOThread <= 0) {
            throw new IllegalArgumentException("numberOfMessageIOThread must be positive.");
        }
        this.numberOfMessageIOThread_ = numberOfMessageIOThread;
        return this;
    }

    public int numberOfMessageIOThread() {
        return numberOfMessageIOThread_;
    }

    public NioDatagramSocketProcessor setReadBufferSize(int readBufferSize) {
        if (readBufferSize <= 0) {
            throw new IllegalArgumentException("readBufferSize must be positive.");
        }
        this.readBufferSize_ = readBufferSize;
        return this;
    }

    public int readBufferSize() {
        return readBufferSize_;
    }

    public NioDatagramSocketProcessor setWriteBufferSize(int writeBufferSize) {
        if (writeBufferSize <= 0) {
            throw new IllegalArgumentException("writeBufferSize must be positive.");
        }
        this.writeBufferSize_ = writeBufferSize;
        return this;
    }

    public int writeBufferSize() {
        return writeBufferSize_;
    }

    public NioDatagramSocketProcessor setUseDirectBuffer(boolean useDirectBuffer) {
        this.useDirectBuffer_ = useDirectBuffer;
        return this;
    }

    public boolean isUseDirectBuffer() {
        return useDirectBuffer_;
    }

    public NioDatagramSocketProcessor setTaskWeightThresholdOfIOSelectorPool(int threshold) {
        ioSelectorPool_.setTaskWeightThreshold(threshold);
        return this;
    }

    public int getTaskWeightThresholdOfIOSelectorPool() {
        return ioSelectorPool_.getTaskWeightThreshold();
    }

    public NioDatagramSocketProcessor setWriteQueueFactory(WriteQueueFactory writeQueueFactory) {
        Objects.requireNonNull(writeQueueFactory, "writeQueueFactory");
        writeQueueFactory_ = writeQueueFactory;
        return this;
    }

    public NioDatagramSocketProcessor setTaskTimer(TaskTimer taskTimer) {
        taskTimer_ = taskTimer;
        return this;
    }
}
