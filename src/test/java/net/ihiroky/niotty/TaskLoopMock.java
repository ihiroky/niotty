package net.ihiroky.niotty;

/**
* @author Hiroki Itoh
*/
class TaskLoopMock extends TaskLoop {

    private final Object mutex_ = new Object();

    protected TaskLoopMock() {
        super(TaskTimer.NULL);
    }

    protected TaskLoopMock(TaskTimer taskTimer) {
        super(taskTimer);
    }

    @Override
    protected void onOpen() {
    }

    @Override
    protected void onClose() {
    }

    @Override
    protected void poll(boolean preferToWait) throws Exception {
        synchronized (mutex_) {
            if (preferToWait) {
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
