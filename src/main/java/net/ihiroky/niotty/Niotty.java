package net.ihiroky.niotty;

/**
 * @author Hiroki Itoh
 */
public final class Niotty {

    private Niotty() {
        throw new AssertionError();
    }

    public static <C extends TransportConfig> Processor<C> newProcessor(
            BusInterface<C> busInterface, PipeLineFactory pipeLineFactory) {
        return new Processor<C>(busInterface, pipeLineFactory);
    }

    public static PipeLine newPipeLine(TransportAggregate transportAggregate) {
        return new DefaultPipeLine(transportAggregate);
    }

    public static PipeLine newPipeLine() {
        return new DefaultPipeLine();
    }

    public static TransportAggregate createTransportAggregate() {
        return new DefaultTransportAggregate();
    }
}
