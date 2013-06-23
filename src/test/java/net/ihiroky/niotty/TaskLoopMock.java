package net.ihiroky.niotty;

/**
* @author Hiroki Itoh
*/
class TaskLoopMock extends TaskLoop<TaskLoopMock> {

    private final Object mutex_ = new Object();

    @Override
    protected void onOpen() {
    }

    @Override
    protected void onClose() {
    }

    @Override
    protected void process(int waitTimeMillis) throws Exception {
        synchronized (mutex_) {
            if (waitTimeMillis > 0) {
                mutex_.wait(waitTimeMillis);
            } else if (waitTimeMillis < 0) {
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
