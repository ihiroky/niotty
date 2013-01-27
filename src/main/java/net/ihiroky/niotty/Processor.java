package net.ihiroky.niotty;

import java.util.Objects;

/**
 * Created on 13/01/16, 17:11
 *
 * @author Hiroki Itoh
 */
public class Processor<C extends TransportConfig> {

    private BusInterface<C> busInterface;

    Processor(BusInterface<C> busInterface, PipeLineFactory pipeLineFactory) {
        Objects.requireNonNull(busInterface, "busInterface");
        Objects.requireNonNull(pipeLineFactory, "pipeLineFactory");

        this.busInterface = busInterface;
        TransportConfig config = busInterface.getConfig();
        config.setPipeLineFactory(pipeLineFactory);
    }

    public C getConfig() {
        return busInterface.getConfig();
    }

    public void start() {
        busInterface.start();
    }

    public void stop() {
        busInterface.stop();
    }

    public Transport createTransport() {
        return busInterface.createTransport();
    }

}
