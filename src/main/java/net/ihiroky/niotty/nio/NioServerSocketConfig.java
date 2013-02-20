package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.Niotty;
import net.ihiroky.niotty.PipeLine;
import net.ihiroky.niotty.TransportConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.SocketOption;
import java.net.StandardSocketOptions;
import java.nio.channels.ServerSocketChannel;
import java.util.Objects;

/**
 * Created on 13/01/15, 18:19
 *
 * @author Hiroki Itoh
 */
public class NioServerSocketConfig extends TransportConfig {

    private int backlog;
    private int receiveBufferSize;
    private boolean reuseAddress;
    private PipeLine contextPipeLine;

    private Logger logger = LoggerFactory.getLogger(NioServerSocketTransport.class);

    public NioServerSocketConfig() {
        backlog = 50;
        reuseAddress = true;
        setPipeLineFactory(Niotty.newEmptyPipeLineFactory(NioServerSocketProcessor.DEFAULT_NAME));
    }

    private <T> void setOption(ServerSocketChannel channel, SocketOption<T> option, T value) {
        try {
            channel.setOption(option, value);
        } catch (IOException ioe) {
            throw new RuntimeException("failed to set socket option:" + option + " value:" + value + " for:" + channel);
        }
    }

    private <T> void logOptionValue(ServerSocketChannel channel, SocketOption<T> option) {
        try {
            logger.info("{}'s {}: {}", channel, option, channel.getOption(option));
        } catch (IOException ioe) {
            throw new RuntimeException("failed to set socket option:" + option + " for:" + channel);
        }
    }

    void applySocketOptions(ServerSocketChannel channel) {
        Objects.requireNonNull(channel, "s");

        if (receiveBufferSize > 0) {
            setOption(channel, StandardSocketOptions.SO_RCVBUF, receiveBufferSize);
        }
        setOption(channel, StandardSocketOptions.SO_REUSEADDR, reuseAddress);

        logOptionValue(channel, StandardSocketOptions.SO_RCVBUF);
        logOptionValue(channel, StandardSocketOptions.SO_REUSEADDR);
    }

    public void setBacklog(int n) {
        backlog = n;
    }

    public int getBacklog() {
        return backlog;
    }

    public void setReceiveBufferSize(int size) {
        receiveBufferSize = size;
    }

    public int getRecieveBufferSize() {
        return receiveBufferSize;
    }

    public void setReuseAddress(boolean on) {
        reuseAddress = on;
    }

    public boolean getReuseAddress() {
        return reuseAddress;
    }

    public void setContextPipeLine(PipeLine pipeLine) {
        contextPipeLine = pipeLine;
    }

    public PipeLine getContextPipeLine() {
        return contextPipeLine;
    }
}
