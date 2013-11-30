package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.DefaultTransportFuture;
import net.ihiroky.niotty.PipelineComposer;
import net.ihiroky.niotty.Transport;
import net.ihiroky.niotty.TransportFuture;
import net.ihiroky.niotty.TransportOption;
import net.ihiroky.niotty.buffer.Packet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Collections;
import java.util.Set;

/**
 * @author Hiroki Itoh
 */
public class ConnectionWaitTransport extends NioSocketTransport<SelectLoop> {

    private final NioClientSocketTransport transport_;
    private final DefaultTransportFuture future_;

    private static Logger logger_ = LoggerFactory.getLogger(ConnectionWaitTransport.class);

    ConnectionWaitTransport(SelectLoopGroup group, NioClientSocketTransport transport, DefaultTransportFuture future) {
        super("ConnectionPending", PipelineComposer.empty(), group);
        transport_ = transport;
        future_ = future;
    }

    @Override
    public TransportFuture bind(SocketAddress local) {
        throw new UnsupportedOperationException();
    }

    @Override
    public TransportFuture connect(SocketAddress local) {
        throw new UnsupportedOperationException();
    }

    @Override
    public TransportFuture close() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void write(Object message) {
        throw new UnsupportedOperationException();
    }

    @Override
    public SocketAddress localAddress() {
        throw new UnsupportedOperationException();
    }

    @Override
    public SocketAddress remoteAddress() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isOpen() {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> Transport setOption(TransportOption<T> option, T value) {
        return this;
    }

    @Override
    public <T> T option(TransportOption<T> option) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<TransportOption<?>> supportedOptions() {
        return Collections.emptySet();
    }

    @Override
    void flush(ByteBuffer writeBuffer) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    void onSelected(SelectionKey key, SelectLoop selectLoop) {
        if (!future_.executing()) {
            return;
        }

        SocketChannel channel = (SocketChannel) key.channel();
        try {
            if (channel.finishConnect()) {
                logger_.info("new channel {} is connected.", channel);
                clearInterestOp(SelectionKey.OP_CONNECT);
                transport_.register(channel, SelectionKey.OP_READ);

                // The done() must be called after register() to ensure that the SelectionKey of IO selector is fixed.
            }
            future_.done();
        } catch (IOException ioe) {
            future_.setThrowable(ioe);
            transport_.closeSelectableChannel();
        }
    }

    @Override
    void readyToWrite(Packet message, Object parameter) {
        throw new UnsupportedOperationException();
    }
}
