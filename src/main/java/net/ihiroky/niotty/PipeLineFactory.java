package net.ihiroky.niotty;

/**
 * Created on 13/01/09, 18:57
 *
 * @author Hiroki Itoh
 */
public abstract class PipeLineFactory {

    public abstract PipeLine createPipeLine();

    protected static PipeLine newPipeLine(Stage<?>...stages) {
        DefaultPipeLine pipeLine = new DefaultPipeLine();
        for (Stage<?> stage : stages) {
            pipeLine.add(stage);
        }
        return pipeLine;
    }
}
