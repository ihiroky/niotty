package net.ihiroky.niotty;

import java.util.concurrent.TimeUnit;

/**
 * Created on 13/01/11, 17:29
 *
 * @author Hiroki Itoh
 */
public class DefaultTransportFuture implements TransportFuture {
    @Override
    public Transport getTransport() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean isCancelled() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean isDone() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Throwable getThrowable() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void await() throws InterruptedException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void await(long timeout, TimeUnit unit) throws InterruptedException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void addListener() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void removeListener() {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
