package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.AbstractProcessor;
import net.ihiroky.niotty.NameCountThreadFactory;
import net.ihiroky.niotty.PipelineComposer;
import net.ihiroky.niotty.util.Arguments;

/**
 * An implementation of {@link net.ihiroky.niotty.Processor} for NIO {@code ServerSocketChannel}.
 */
public class NioServerSocketProcessor extends AbstractProcessor<NioServerSocketTransport> {

    private AcceptSelectorPool acceptSelectorPool_;
    private TcpIOSelectorPool ioSelectorPool_;
    private WriteQueueFactory writeQueueFactory_;

    private int numberOfAcceptThread_;
    private int numberOfMessageIOThread_;
    private int readBufferSize_;
    private boolean useDirectBuffer_;
    private boolean duplicateReadBuffer_;

    private static final int DEFAULT_NUMBER_OF_ACCEPT_THREAD = 1;
    private static final int DEFAULT_NUMBER_OF_MESSAGE_IO_THREAD =
            Math.max(Runtime.getRuntime().availableProcessors() / 2, 2);

    static final String DEFAULT_NAME = "NioServerSocket";

    public NioServerSocketProcessor() {
        writeQueueFactory_ = new SimpleWriteQueueFactory();

        numberOfAcceptThread_ = DEFAULT_NUMBER_OF_ACCEPT_THREAD;
        numberOfMessageIOThread_ = DEFAULT_NUMBER_OF_MESSAGE_IO_THREAD;
        readBufferSize_ = TcpIOSelectorPool.DEFAULT_READ_BUFFER_SIZE;
        useDirectBuffer_ = TcpIOSelectorPool.DEFAULT_USE_DIRECT_BUFFER;
        duplicateReadBuffer_ = TcpIOSelectorPool.DEFAULT_DUPLICATE_READ_BUFFER;

        setName(DEFAULT_NAME);
    }

    @Override
    protected void onStart() {
        ioSelectorPool_ = new TcpIOSelectorPool(
                new NameCountThreadFactory(name().concat("-MessageIO")), numberOfMessageIOThread_,
                readBufferSize_, useDirectBuffer_, duplicateReadBuffer_);
        acceptSelectorPool_ = new AcceptSelectorPool(new NameCountThreadFactory(name().concat("-Accept")));
    }

    @Override
    protected void onStop() {
        acceptSelectorPool_.close();
        ioSelectorPool_.close();
    }

    @Override
    public NioServerSocketTransport createTransport() {
        return new NioServerSocketTransport(this);
    }

    @Override
    public NioServerSocketProcessor setName(String name) {
        super.setName(name);
        return this;
    }

    @Override
    public NioServerSocketProcessor setPipelineComposer(PipelineComposer composer) {
        super.setPipelineComposer(composer);
        return this;
    }

    public NioServerSocketProcessor setNumberOfAcceptThread(int numberOfAcceptThread) {
        this.numberOfAcceptThread_ = Arguments.requirePositive(numberOfAcceptThread, "numberOfAcceptThread");
        return this;
    }

    public NioServerSocketProcessor setNumberOfMessageIOThread(int numberOfMessageIOThread) {
        this.numberOfMessageIOThread_ = Arguments.requirePositive(numberOfMessageIOThread, "numberofMessageIOThread");
        return this;
    }

    public NioServerSocketProcessor setReadBufferSize(int readBufferSize) {
        readBufferSize_ = Arguments.requirePositive(readBufferSize, "readBufferSize");
        return this;
    }

    public NioServerSocketProcessor setUseDirectBuffer(boolean useDirectBuffer) {
        useDirectBuffer_ = useDirectBuffer;
        return this;
    }

    public NioServerSocketProcessor setDuplicateReadBuffer(boolean duplicateReadBuffer) {
        duplicateReadBuffer_ = duplicateReadBuffer;
        return this;
    }

    public NioServerSocketProcessor setWriteQueueFactory(WriteQueueFactory writeQueueFactory) {
        writeQueueFactory_ = writeQueueFactory;
        return this;
    }

    AcceptSelectorPool acceptSelectorPool() {
        return acceptSelectorPool_;
    }

    TcpIOSelectorPool ioSelectorPool() {
        return ioSelectorPool_;
    }

    WriteQueueFactory writeQueueFactory() {
        return writeQueueFactory_;
    }

    public int numberOfAcceptThread() {
        return numberOfAcceptThread_;
    }

    public int numberOfMessageIOThread() {
        return numberOfMessageIOThread_;
    }

    public int readBufferSize() {
        return readBufferSize_;
    }

    public boolean useDirectBuffer() {
        return useDirectBuffer_;
    }

    public boolean duplicateReadBuffer() {
        return duplicateReadBuffer_;
    }
}
