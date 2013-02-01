package net.ihiroky.niotty;

/**
 * Created on 13/01/15, 18:27
 *
 * @author Hiroki Itoh
 */
public class TransportConfig {

    private volatile PipeLineFactory pipeLineFactory;

    public TransportConfig() {
    }

    public PipeLineFactory getPipeLineFactory() {
        return pipeLineFactory;
    }

    protected void setPipeLineFactory(PipeLineFactory factory) {
        pipeLineFactory = factory;
    }
}
