package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.PipeLineFactory;
import net.ihiroky.niotty.TransportConfig;

import java.net.Socket;
import java.util.Objects;

/**
 * Created on 13/01/17, 18:01
 *
 * @author Hiroki Itoh
 */
public class NioClientSocketConfig extends TransportConfig {

    private int numberOfConnectThread;
    private int numberOfMessageIOThread;

    public NioClientSocketConfig(PipeLineFactory pipeLineFactory) {
        Objects.requireNonNull(pipeLineFactory, "pipeLineFactory");

        setPipeLineFactory(pipeLineFactory);
        numberOfConnectThread = 1;
        numberOfMessageIOThread = Math.max(Runtime.getRuntime().availableProcessors() / 2, 2);
    }

    void applySocketOptions(Socket s) {

    }

    public int getNumberOfConnectThread() {
        return numberOfConnectThread;
    }

    public void setNumberOfConnectThread(int numberOfConnectThread) {
        this.numberOfConnectThread = numberOfConnectThread;
    }

    public int getNumberOfMessageIOThread() {
        return numberOfMessageIOThread;
    }

    public void setNumberOfMessageIOThread(int numberOfMessageIOThread) {
        this.numberOfMessageIOThread = numberOfMessageIOThread;
    }
}
