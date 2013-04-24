package net.ihiroky.niotty;

/**
 * Created on 13/01/16, 17:11
 *
 * @author Hiroki Itoh
 */
public interface Processor<T extends Transport, C> {
    void start();
    void stop();
    String name();
    void setPipelineComposer(PipelineComposer composer);
    T createTransport(C config);
}
