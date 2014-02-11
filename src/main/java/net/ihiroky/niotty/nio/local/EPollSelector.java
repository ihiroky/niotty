package net.ihiroky.niotty.nio.local;

import net.ihiroky.niotty.util.Arguments;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.nio.channels.spi.AbstractSelector;
import java.util.Set;

/**
 *
 */
public class EPollSelector extends AbstractSelector {

    private final int fd_;
    private final Native.EPollEvent tmpEvent_;
    private final Native.EPollEvent eventsHead_;
    private final Native.EPollEvent[] eventBuffer_;

    private static final int EVENT_BUFFER_SIZE = 512;

    private  EPollSelector(int eventBufferSize) throws IOException {
        super(null);

        Arguments.requirePositive(eventBufferSize, "eventBufferSize");
        int fd = Native.epoll_create(eventBufferSize);
        if (fd == -1) {
            throw new IOException(Native.getLastError());
        }

        Native.EPollEvent.ByReference tmpEvent = new Native.EPollEvent.ByReference(fd, Native.EPOLLIN);
        Native.EPollEvent eventsHead = new Native.EPollEvent();
        Native.EPollEvent[] eventBuffer = (Native.EPollEvent[]) eventsHead.toArray(eventBufferSize);

        fd_ = fd;
        tmpEvent_ = tmpEvent;
        eventsHead_ = eventsHead;
        eventBuffer_ = eventBuffer;
    }

    public static EPollSelector open() throws IOException {
        return new EPollSelector(EVENT_BUFFER_SIZE);
    }

    public static EPollSelector open(int eventBufferSize) throws IOException {
        return new EPollSelector(eventBufferSize);
    }

    @Override
    protected void implCloseSelector() throws IOException {
        if (Native.close(fd_) == -1) {
            throw new IOException(Native.getLastError());
        }
    }

    @Override
    protected SelectionKey register(AbstractSelectableChannel ch, int ops, Object att) {
        return null;
    }

    @Override
    public Set<SelectionKey> keys() {
        return null;
    }

    @Override
    public Set<SelectionKey> selectedKeys() {
        return null;
    }

    @Override
    public int selectNow() throws IOException {
        return 0;
    }

    @Override
    public int select(long timeout) throws IOException {
        return 0;
    }

    @Override
    public int select() throws IOException {
        return 0;
    }

    @Override
    public Selector wakeup() {
        return null;
    }
}
