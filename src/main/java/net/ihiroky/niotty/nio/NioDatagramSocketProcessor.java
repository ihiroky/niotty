package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.AbstractProcessor;
import net.ihiroky.niotty.EventDispatcherGroup;
import net.ihiroky.niotty.NameCountThreadFactory;
import net.ihiroky.niotty.PipelineComposer;
import net.ihiroky.niotty.util.Arguments;

/**
 * An implementation of {@link net.ihiroky.niotty.Processor} for NIO {@code DatagramChannel}.
 */
public class NioDatagramSocketProcessor extends AbstractProcessor<NioDatagramSocketTransport> {

    private EventDispatcherGroup<SelectDispatcher> ioSelectDispatcherGroup_;
    private int numberOfMessageIOThread_;
    private WriteQueueFactory<DatagramQueue> writeQueueFactory_;

    private int readBufferSize_;
    private int writeBufferSize_;
    private boolean useDirectBuffer_;

    private static final int DEFAULT_NUMBER_OF_MESSAGE_IO_THREAD = 1;

    private static final String DEFAULT_NAME = "NioDatagramSocket";

    public NioDatagramSocketProcessor() {
        numberOfMessageIOThread_ = DEFAULT_NUMBER_OF_MESSAGE_IO_THREAD;
        writeQueueFactory_ = new SimpleDatagramQueueFactory();

        readBufferSize_ = SelectDispatcherFactory.DEFAULT_READ_BUFFER_SIZE;
        writeBufferSize_ = SelectDispatcherFactory.DEFAULT_WRITE_BUFFER_SIZE;
        useDirectBuffer_ = SelectDispatcherFactory.DEFAULT_USE_DIRECT_BUFFER;
        setName(DEFAULT_NAME);
    }

    @Override
    protected void onStart() {
        SelectDispatcherFactory ioDispatcherFactory = new SelectDispatcherFactory()
                .setReadBufferSize(readBufferSize_)
                .setWriteBufferSize(writeBufferSize_)
                .setUseDirectBuffer(useDirectBuffer_);
        ioSelectDispatcherGroup_ = new EventDispatcherGroup<SelectDispatcher>(
                numberOfMessageIOThread_, new NameCountThreadFactory(name().concat("-IO")), ioDispatcherFactory);
    }

    @Override
    protected void onStop() {
        ioSelectDispatcherGroup_.close();
    }

    @Override
    public NioDatagramSocketTransport createTransport() {
        return new NioDatagramSocketTransport(
                name(), pipelineComposer(), ioSelectDispatcherGroup_, writeQueueFactory_, (InternetProtocolFamily) null);
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
                name(), pipelineComposer(), ioSelectDispatcherGroup_, writeQueueFactory_, family);
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
        this.numberOfMessageIOThread_ = Arguments.requirePositive(numberOfMessageIOThread, "numberOfMessageIOThread");
        return this;
    }

    public int numberOfMessageIOThread() {
        return numberOfMessageIOThread_;
    }

    public NioDatagramSocketProcessor setReadBufferSize(int readBufferSize) {
        readBufferSize_ = Arguments.requirePositive(readBufferSize, "readBufferSize");
        return this;
    }

    public int readBufferSize() {
        return readBufferSize_;
    }

    public NioDatagramSocketProcessor setWriteBufferSize(int writeBufferSize) {
        writeBufferSize_ = Arguments.requirePositive(writeBufferSize, "writeBufferSize");
        return this;
    }

    public int writeBufferSize() {
        return writeBufferSize_;
    }

    public NioDatagramSocketProcessor setUseDirectBuffer(boolean useDirectBuffer) {
        useDirectBuffer_ = useDirectBuffer;
        return this;
    }

    public boolean useDirectBuffer() {
        return useDirectBuffer_;
    }

    public NioDatagramSocketProcessor setWriteQueueFactory(WriteQueueFactory<DatagramQueue> writeQueueFactory) {
        writeQueueFactory_ = Arguments.requireNonNull(writeQueueFactory, "writeQueueFactory");
        return this;
    }
}
