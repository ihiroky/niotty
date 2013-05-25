package net.ihiroky.niotty.nio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.SocketOption;
import java.net.StandardSocketOptions;
import java.nio.channels.ServerSocketChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Created on 13/01/15, 18:19
 *
 * @author Hiroki Itoh
 */
public class NioServerSocketConfig {

    private int backlog_;
    private Map<SocketOption<?>, Object> socketOptionMap_;
    private WriteQueueFactory writeQueueFactory_;

    private Logger logger_ = LoggerFactory.getLogger(NioServerSocketTransport.class);

    public NioServerSocketConfig() {
        backlog_ = 50;
        socketOptionMap_ = new HashMap<>();
        writeQueueFactory_ = new SimpleWriteQueueFactory();
    }
    public <T> void setOption(SocketOption<T> name, Object value) {
        socketOptionMap_.put(name, value);
    }

    public <T> T getOption(SocketOption<T> name) {
        Object value = socketOptionMap_.get(name);
        return name.type().cast(value);
    }

    private <T> void logOptionValue(ServerSocketChannel channel, SocketOption<T> option) throws IOException {
        logger_.info("{}'s {}: {}", channel, option, channel.getOption(option));
    }

    void applySocketOptions(ServerSocketChannel channel) {
        try {
            for (Map.Entry<SocketOption<?>, Object> entry : socketOptionMap_.entrySet()) {
                @SuppressWarnings("unchecked")
                SocketOption<Object> name = (SocketOption<Object>) entry.getKey();
                channel.setOption(name, entry.getValue());
            }

            logOptionValue(channel, StandardSocketOptions.SO_RCVBUF);
            logOptionValue(channel, StandardSocketOptions.SO_REUSEADDR);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    public void setBacklog(int n) {
        backlog_ = n;
    }

    public int getBacklog() {
        return backlog_;
    }

    public void setWriteQueueFactory(WriteQueueFactory writeQueueFactory) {
        Objects.requireNonNull(writeQueueFactory, "writeQueueFactory");
        this.writeQueueFactory_ = writeQueueFactory;
    }

    public WriteQueue newWriteQueue() {
        return writeQueueFactory_.newWriteQueue();
    }
}
