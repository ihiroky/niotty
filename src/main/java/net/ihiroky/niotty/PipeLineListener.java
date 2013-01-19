package net.ihiroky.niotty;

/**
 * Created on 13/01/16, 18:21
 *
 * @author Hiroki Itoh
 */
public interface PipeLineListener {

    void onFire(PipeLine pipeLine, TransportEvent event);
    void onFinish(PipeLine pipeLine, TransportEvent event);
}
