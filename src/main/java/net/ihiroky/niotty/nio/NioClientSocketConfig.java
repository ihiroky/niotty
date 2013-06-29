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
 * A configuration for {@code java.nio.channels.ServerChannel}.
 *
 * @author Hiroki Itoh
 */
public class NioClientSocketConfig {

    private Map<SocketOption<?>, Object> socketOptionMap_;
    private WriteQueueFactory writeQueueFactory_;

    private Logger logger_ = LoggerFactory.getLogger(NioClientSocketConfig.class);

    /**
     * Constructs a new instance
     */
    public NioClientSocketConfig() {
        socketOptionMap_ = new HashMap<>();
        writeQueueFactory_ = new SimpleWriteQueueFactory();
    }

    /**
     * Sets a socket option.
     *
     * @param name a name of the option
     * @param value a value of the option
     * @param <T> a type of the value
     * @return this config
     */
    public <T> NioClientSocketConfig setOption(SocketOption<T> name, Object value) {
        socketOptionMap_.put(name, value);
        return this;
    }

    /**
     * Returns a socket option.
     *
     * @param name a name of the option
     * @param <T> a type of the option's value
     * @return the value of the option
     */
    public <T> T option(SocketOption<T> name) {
        Object value = socketOptionMap_.get(name);
        return name.type().cast(value);
    }

    private <T> void logOptionValue(SocketChannel channel, SocketOption<T> option) throws IOException {
        logger_.info("{}@{}'s {}: {}", channel, channel.hashCode(), option, channel.getOption(option));
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

    /**
     * Sets a write queue factory for a socket.
     *
     * @param factory the write queue factory
     * @return this config
     */
    public NioClientSocketConfig setWriteQueueFactory(WriteQueueFactory factory) {
        writeQueueFactory_ = factory;
        return this;
    }

    WriteQueue newWriteQueue() {
        return writeQueueFactory_.newWriteQueue();
    }
}
