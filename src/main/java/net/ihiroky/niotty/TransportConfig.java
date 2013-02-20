package net.ihiroky.niotty;

import java.util.Objects;

/**
 * Created on 13/01/15, 18:27
 *
 * @author Hiroki Itoh
 */
public class TransportConfig {

    private volatile PipeLineFactory pipeLineFactory;

    public PipeLineFactory getPipeLineFactory() {
        return pipeLineFactory;
    }

    public void setPipeLineFactory(PipeLineFactory pipeLineFactory) {
        Objects.requireNonNull(pipeLineFactory, "pipeLineFactory");
        this.pipeLineFactory = pipeLineFactory;
    }
}
