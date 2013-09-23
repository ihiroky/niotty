package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.AbstractProcessor;
import net.ihiroky.niotty.NameCountThreadFactory;
import net.ihiroky.niotty.PipelineComposer;
import net.ihiroky.niotty.util.Objects;

/**
 * An implementation of {@link net.ihiroky.niotty.Processor} for NIO {@code DatagramChannel}.
 */
public class NioDatagramSocketProcessor extends AbstractProcessor<NioDatagramSocketTransport> {

    private UdpIOSelectorPool ioSelectorPool_;
    private int numberOfMessageIOThread_;
    private WriteQueueFactory writeQueueFactory_;

    private static final int DEFAULT_NUMBER_OF_MESSAGE_IO_THREAD = 1;

    private static final String DEFAULT_NAME = "NioDatagramSocket";

    public NioDatagramSocketProcessor() {
        ioSelectorPool_ = new UdpIOSelectorPool();
        numberOfMessageIOThread_ = DEFAULT_NUMBER_OF_MESSAGE_IO_THREAD;
        writeQueueFactory_ = new SimpleWriteQueueFactory();
        setName(DEFAULT_NAME);
    }

    @Override
    protected void onStart() {
        ioSelectorPool_.open(new NameCountThreadFactory(name().concat("-IO")), numberOfMessageIOThread_);
    }

    @Override
    protected void onStop() {
        ioSelectorPool_.close();
    }

    @Override
    public NioDatagramSocketTransport createTransport() {
        return new NioDatagramSocketTransport(
                null, pipelineComposer(), name(), ioSelectorPool_, writeQueueFactory_);
    }

    /**
     * Constructs the transport.
     * The {@code protocolFamily} parameter is used to specify the {@code InternetProtocolFamily}.
     * If the datagram channel is to be used for IP multicasing then this should correspond to
     * the address type of the multicast groups that this channel will join.
     *
     * @param family the protocolFamily
     * @return the transport.
     */
    public NioDatagramSocketTransport createTransport(InternetProtocolFamily family) {
        return new NioDatagramSocketTransport(
                family, pipelineComposer(), name(), ioSelectorPool_, writeQueueFactory_);
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
        ioSelectorPool_.setReadBufferSize(readBufferSize);
        return this;
    }

    public int readBufferSize() {
        return ioSelectorPool_.readBufferSize();
    }

    public NioDatagramSocketProcessor setWriteBufferSize(int writeBufferSize) {
        if (writeBufferSize <= 0) {
            throw new IllegalArgumentException("writeBufferSize must be positive.");
        }
        ioSelectorPool_.setWriteBufferSize(writeBufferSize);
        return this;
    }

    public int writeBufferSize() {
        return ioSelectorPool_.writeBufferSize();
    }

    public NioDatagramSocketProcessor setUseDirectBuffer(boolean useDirectBuffer) {
        ioSelectorPool_.setDirect(useDirectBuffer);
        return this;
    }

    public boolean isUseDirectBuffer() {
        return ioSelectorPool_.direct();
    }

    public boolean duplicateReceiveBuffer() {
        return ioSelectorPool_.duplicateBuffer();
    }

    public NioDatagramSocketProcessor setDuplicateReceiveBuffer(boolean duplicateReceiveBuffer) {
        ioSelectorPool_.setDuplicateBuffer(duplicateReceiveBuffer);
        return this;
    }

    public NioDatagramSocketProcessor setWriteQueueFactory(WriteQueueFactory writeQueueFactory) {
        Objects.requireNonNull(writeQueueFactory, "writeQueueFactory");
        writeQueueFactory_ = writeQueueFactory;
        return this;
    }
}
