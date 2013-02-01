package net.ihiroky.niotty;

/**
 * @author Hiroki Itoh
 */
public final class Niotty {

    private static final TransportAggregate NULL_TRANSPORT_AGGREGATE = new NullTransportAggregate();

    private Niotty() {
        throw new AssertionError();
    }

    public static PipeLine newPipeLine(Stage<?> ...stages) {
        PipeLine pipeLine = new DefaultPipeLine();
        for (Stage<?> stage : stages) {
            pipeLine.add(stage);
        }
        return pipeLine;
    }

    public static TransportAggregate newTransportAggregate() {
        return new DefaultTransportAggregate();
    }

    public static TransportAggregate newContextTransportAggregate(PipeLine pipeLine) {
        return new ContextTransportAggregate(pipeLine);
    }

    public static TransportAggregate getNullTransportAggregate() {
        return NULL_TRANSPORT_AGGREGATE;
    }

    private static class NullTransportAggregate implements TransportAggregate {
        @Override
        public void write(Object message) {
        }

        @Override
        public void close() {
        }

        @Override
        public void add(Transport transport) {
        }

        @Override
        public void remove(Transport transport) {
        }
    }
}
