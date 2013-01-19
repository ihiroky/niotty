package net.ihiroky.niotty.nio;

import net.ihiroky.niotty.TransportConfig;

import java.net.Socket;

/**
 * Created on 13/01/17, 18:01
 *
 * @author Hiroki Itoh
 */
public class NioClientSocketConfig extends TransportConfig {

    private int numberOfConnectThread;
    private int numberOfMessageIOThread;

    public NioClientSocketConfig() {
        numberOfConnectThread = 1;
        numberOfMessageIOThread = Runtime.getRuntime().availableProcessors() / 2;
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
