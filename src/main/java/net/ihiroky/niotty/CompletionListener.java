package net.ihiroky.niotty;

/**
 * @author Hiroki Itoh
 */
public interface CompletionListener {

    void onComplete(TransportFuture future);
}
