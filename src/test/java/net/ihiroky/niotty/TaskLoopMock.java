package net.ihiroky.niotty;

import java.util.concurrent.TimeUnit;

/**
* @author Hiroki Itoh
*/
class TaskLoopMock extends TaskLoop {

    private final Object mutex_ = new Object();

    @Override
    protected void onOpen() {
    }

    @Override
    protected void onClose() {
    }

    @Override
    protected void process(long timeout, TimeUnit timeUnit) throws Exception {
        synchronized (mutex_) {
            if (timeout > 0) {
                mutex_.wait(timeUnit.toMillis(timeout));
            } else if (timeout < 0) {
                mutex_.wait();
            }
        }
    }

    @Override
    protected void wakeUp() {
        synchronized (mutex_) {
            mutex_.notifyAll();
        }
    }
}
