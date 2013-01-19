package net.ihiroky.niotty;

import java.util.Objects;

/**
 * Created on 13/01/16, 17:11
 *
 * @author Hiroki Itoh
 */
public class CoreProcessor<C extends TransportConfig> {

    private BusInterface<C> busInterface;

    CoreProcessor(BusInterface<C> busInterface,
                         PipeLineFactory loadPipeLineFactory,
                         PipeLineFactory storePipeLineFactory) {
        Objects.requireNonNull(busInterface, "busInterface");
        Objects.requireNonNull(loadPipeLineFactory, "loadPipeLineFactory");
        Objects.requireNonNull(storePipeLineFactory, "storePipeLineFactory");

        this.busInterface = busInterface;
        TransportConfig config = busInterface.getConfig();
        config.setLoadPipeLineFactory(loadPipeLineFactory);
        config.setStorePipeLineFactory(storePipeLineFactory);
    }

    public static <C extends TransportConfig> CoreProcessor<C> newCoreProcessor(
            BusInterface<C> busInterface, PipeLineFactory loadPipeLineFactory, PipeLineFactory storePipeLineFactory) {
        return new CoreProcessor<C>(busInterface, loadPipeLineFactory, storePipeLineFactory);
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
