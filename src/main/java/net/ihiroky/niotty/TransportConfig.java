package net.ihiroky.niotty;

import java.util.Objects;

/**
 * Created on 13/01/15, 18:27
 *
 * @author Hiroki Itoh
 */
public class TransportConfig {

    private volatile String baseName;
    private volatile PipeLineFactory pipeLineFactory;

    public TransportConfig() {
    }

    public String getBaseName() {
        return baseName;
    }

    public void setBaseName(String n) {
        Objects.requireNonNull(n, "n");
        baseName = n;
    }

    public PipeLineFactory getPipeLineFactory() {
        return pipeLineFactory;
    }

    void setPipeLineFactory(PipeLineFactory factory) {
        pipeLineFactory = factory;
    }
}
