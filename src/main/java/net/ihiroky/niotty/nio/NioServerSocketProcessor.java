package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.AbstractProcessor;
import net.ihiroky.niotty.NameCountThreadFactory;
import net.ihiroky.niotty.PipelineComposer;

/**
 * An implementation of {@link net.ihiroky.niotty.Processor} for NIO {@code ServerSocketChannel}.
 */
public class NioServerSocketProcessor extends AbstractProcessor<NioServerSocketTransport> {

    private AcceptSelectorPool acceptSelectorPool_;
    private TcpIOSelectorPool ioSelectorPool_;
    private WriteQueueFactory writeQueueFactory_;
    private int readBufferSize_;
    private int writeBufferSize_;
    private boolean useDirectBuffer_;

    private int numberOfAcceptThread_;
    private int numberOfMessageIOThread_;

    private static final int DEFAULT_NUMBER_OF_ACCEPT_THREAD = 1;
    private static final int DEFAULT_NUMBER_OF_MESSAGE_IO_THREAD =
            Math.max(Runtime.getRuntime().availableProcessors() / 2, 2);
    private static final int DEFAULT_BUFFER_SIZE = 8192;
    private static final boolean DEFAULT_DIRECT_BUFFER = true;

    static final String DEFAULT_NAME = "NioServerSocket";

    public NioServerSocketProcessor() {
        acceptSelectorPool_ = new AcceptSelectorPool();
        ioSelectorPool_ = new TcpIOSelectorPool();
        writeQueueFactory_ = new SimpleWriteQueueFactory();

        numberOfAcceptThread_ = DEFAULT_NUMBER_OF_ACCEPT_THREAD;
        numberOfMessageIOThread_ = DEFAULT_NUMBER_OF_MESSAGE_IO_THREAD;
        readBufferSize_ = DEFAULT_BUFFER_SIZE;
        writeBufferSize_ = DEFAULT_BUFFER_SIZE;
        useDirectBuffer_ = DEFAULT_DIRECT_BUFFER;

        setName(DEFAULT_NAME);
    }

    @Override
    protected void onStart() {
        ioSelectorPool_.setReadBufferSize(readBufferSize_);
        ioSelectorPool_.setDirect(useDirectBuffer_);
        ioSelectorPool_.open(new NameCountThreadFactory(name().concat("-MessageIO")), numberOfMessageIOThread_);

        acceptSelectorPool_.open(new NameCountThreadFactory(name().concat("-Accept")), numberOfAcceptThread_);
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
        if (numberOfAcceptThread <= 0) {
            throw new IllegalArgumentException("numberOfAcceptThread must be positive.");
        }
        this.numberOfAcceptThread_ = numberOfAcceptThread;
        return this;
    }

    public NioServerSocketProcessor setNumberOfMessageIOThread(int numberOfMessageIOThread) {
        if (numberOfMessageIOThread <= 0) {
            throw new IllegalArgumentException("numberOfMessageIOThread must be positive.");
        }
        this.numberOfMessageIOThread_ = numberOfMessageIOThread;
        return this;
    }

    public NioServerSocketProcessor setReadBufferSize(int readBufferSize) {
        if (readBufferSize <= 0) {
            throw new IllegalArgumentException("readBufferSize must be positive.");
        }
        this.readBufferSize_ = readBufferSize;
        return this;
    }

    public NioServerSocketProcessor setWriteBufferSize(int writeBufferSize) {
        if (writeBufferSize <= 0) {
            throw new IllegalArgumentException("writeBufferSize must be positive.");
        }
        this.writeBufferSize_ = writeBufferSize;
        return this;
    }

    public NioServerSocketProcessor setUseDirectBuffer(boolean useDirectBuffer) {
        this.useDirectBuffer_ = useDirectBuffer;
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

    WriteQueueFactory writeQueueFactory(){
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

    public int writeBufferSize() {
        return writeBufferSize_;
    }

    public boolean isUseDirectBuffer() {
        return useDirectBuffer_;
    }
}
