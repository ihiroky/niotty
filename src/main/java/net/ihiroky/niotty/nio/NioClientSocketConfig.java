package net.ihiroky.niotty.nio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.SocketOption;
import java.net.StandardSocketOptions;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;

/**
 * Created on 13/01/17, 18:01
 *
 * @author Hiroki Itoh
 */
public class NioClientSocketConfig {

    private Map<SocketOption<?>, Object> socketOptionMap_;
    private WriteQueueFactory writeQueueFactory_;

    private Logger logger_ = LoggerFactory.getLogger(NioClientSocketConfig.class);

    public NioClientSocketConfig() {
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

    private <T> void logOptionValue(SocketChannel channel, SocketOption<T> option) throws IOException {
        logger_.info("{}'s {}: {}", channel, option, channel.getOption(option));
    }

    void applySocketOptions(SocketChannel channel) {
        try {
            for (Map.Entry<SocketOption<?>, Object> entry : socketOptionMap_.entrySet()) {
                @SuppressWarnings("unchecked")
                SocketOption<Object> name = (SocketOption<Object>) entry.getKey();
                channel.setOption(name, entry.getValue());
            }

            logOptionValue(channel, StandardSocketOptions.SO_SNDBUF);
            logOptionValue(channel, StandardSocketOptions.SO_RCVBUF);
            logOptionValue(channel, StandardSocketOptions.SO_LINGER);
            logOptionValue(channel, StandardSocketOptions.SO_REUSEADDR);
            logOptionValue(channel, StandardSocketOptions.SO_KEEPALIVE);
            logOptionValue(channel, StandardSocketOptions.TCP_NODELAY);
        } catch (IOException ioe) {
            throw new RuntimeException("failed to apply socket options.", ioe);
        }
    }

    WriteQueue newWriteQueue() {
        return writeQueueFactory_.newWriteQueue();
    }
}
