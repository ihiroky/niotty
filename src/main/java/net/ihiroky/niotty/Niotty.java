package net.ihiroky.niotty;

import java.util.Objects;

/**
 * @author Hiroki Itoh
 */
public final class Niotty {

    private Niotty() {
        throw new AssertionError();
    }

    public static PipeLine newPipeLine(String name, Stage<?, ?> ...stages) {
        DefaultPipeLine pipeLine = new DefaultPipeLine(name);
        for (Stage<?, ?> stage : stages) {
            pipeLine.add(stage);
        }
        pipeLine.verifyStageContextType();
        return pipeLine;
    }

    public static PipeLineFactory newEmptyPipeLineFactory(String name) {
        Objects.requireNonNull(name, "name");
        if (name.isEmpty()) {
            throw new IllegalArgumentException("non empty name is required.");
        }
        return new EmptyPipeLineFactory(name);
    }

    private static class EmptyPipeLineFactory implements PipeLineFactory {

        String name;

        EmptyPipeLineFactory(String name) {
            this.name = name;
        }

        @Override
        public PipeLine createLoadPipeLine() {
            return newPipeLine(name.concat("-load"));
        }

        @Override
        public PipeLine createStorePipeLine() {
            return newPipeLine(name.concat("-store"));
        }
    }
}
