package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.TransportConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Objects;

/**
 * Created on 13/01/15, 18:19
 *
 * @author Hiroki Itoh
 */
public class NioServerSocketConfig extends TransportConfig {

    private int backlog;
    private int numberOfAcceptThread;
    private int numberOfMessageIOThread;
    private int childReadBufferSize;
    private boolean direct;
    private int ppConnectionTime;
    private int ppLatency;
    private int ppBandwidth;
    private int receiveBufferSize;
    private boolean reuseAddress;
    private int soTimeout;

    private Logger logger = LoggerFactory.getLogger(NioServerSocketTransport.class);

    NioServerSocketConfig() {
        backlog = 50;
        childReadBufferSize = 8192;
        direct = false;
        numberOfAcceptThread = 1;
        numberOfMessageIOThread = Math.max(Runtime.getRuntime().availableProcessors() / 2, 2);
        reuseAddress = true;
    }

    void applySocketOptions(ServerSocket s) {
        Objects.requireNonNull(s, "s");

        if (ppConnectionTime > 0 || ppLatency > 0 || ppBandwidth > 0) {
            s.setPerformancePreferences(ppConnectionTime, ppLatency, ppBandwidth);
        }
        if (receiveBufferSize > 0) {
            try {
                s.setReceiveBufferSize(receiveBufferSize);
                logger.info("{}'s receiveBufferSize: {}", s, s.getReceiveBufferSize());
            } catch (IOException ioe) {
                throw new RuntimeException("failed to set ReceiveBufferSize", ioe);
            }
        }
        try {
            s.setReuseAddress(reuseAddress);
            logger.info("{}'s reuseAddress: {}", s, s.getReuseAddress());
        } catch (IOException ioe) {
            throw new RuntimeException("failed to set ReuseAddress", ioe);
        }
        if (soTimeout > 0) {
            try {
                s.setSoTimeout(soTimeout);
                logger.info("{}'s soTimeout: {}", s, s.getSoTimeout());
            } catch (IOException ioe) {
                throw new RuntimeException("failed to set SoTimeout", ioe);
            }
        }
    }

    public void setBacklog(int n) {
        backlog = n;
    }

    public int getBacklog() {
        return backlog;
    }

    public void setPerformancePreferences(int connectionTime, int latency, int bandwidth) {
        ppConnectionTime = connectionTime;
        ppLatency = latency;
        ppBandwidth = bandwidth;
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

    public void setSoTimeout(int timeout) {
        soTimeout = timeout;
    }

    public int getSoTimeout() {
        return soTimeout;
    }

    public void setNumberOfAcceptThread(int n) {
        if (n <= 0) {
            throw new IllegalArgumentException("n must be positive.");
        }
        numberOfAcceptThread = n;
    }

    public int getNumberOfAcceptThread() {
        return numberOfAcceptThread;
    }

    public void setNumberOfMessageIOThread(int n) {
        if (n <= 0) {
            throw new IllegalArgumentException("n must be positive.");
        }
        numberOfMessageIOThread = n;
    }

    public int getNumberOfMessageIOThread() {
        return numberOfMessageIOThread;
    }

    public void setChildReadBufferSize(int s) {
        if (s <= 0) {
            throw new IllegalArgumentException("s must be positive.");
        }
        childReadBufferSize = s;
    }

    public int getChildReadBufferSize() {
        return childReadBufferSize;
    }

    public void setDirect(boolean on) {
        direct = on;
    }

    public boolean isDirect() {
        return direct;
    }
}
