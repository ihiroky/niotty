package net.ihiroky.niotty.sample;

/**
* @author Hiroki Itoh
*/
public class Waiter {

    private boolean finished_ = false;

    synchronized void waitUntilFinished() throws InterruptedException {
        while (!finished_) {
            wait();
        }
    }

    synchronized void finished() {
        finished_ = true;
        notifyAll();
    }
}
