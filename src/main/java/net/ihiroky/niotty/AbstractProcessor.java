package net.ihiroky.niotty;

import java.util.Objects;

/**
 * <p>A skeletal implementation of {@link Processor}.</p>
 *
 * <p>This class has a name and a pipeline composer.</p>
 */
public abstract class AbstractProcessor<T extends Transport> implements Processor<T> {

    private String name_;
    private PipelineComposer pipelineComposer_;

    private static final String DEFAULT_NAME = "Processor";

    /**
     * <p>Constructs this instance.</p>
     *
     * <p>The name is set to "Processor" and the pipeline is empty composer, which is composes nothing.</p>
     */
    protected AbstractProcessor() {
        name_ = DEFAULT_NAME;
        pipelineComposer_ = PipelineComposer.empty();
    }

    @Override
    public void start() {
        pipelineComposer_.setUp();
        onStart();
    }

    @Override
    public void stop() {
        try {
            onStop();
        } finally {
            pipelineComposer_.close();
        }
    }

    @Override
    public String name() {
        return name_;
    }

    @Override
    public AbstractProcessor<T> setPipelineComposer(PipelineComposer composer) {
        Objects.requireNonNull(composer, "composer");
        pipelineComposer_ = composer;
        return this;
    }

    /**
     * <p>Returns the pipeline composer.</p>
     * @return the pipeline composer.
     */
    public PipelineComposer pipelineComposer() {
        return pipelineComposer_;
    }

    /**
     * <p>Sets a name of this instance.</p>
     * @param name a name of this instance
     */
    public AbstractProcessor<T> setName(String name) {
        Objects.requireNonNull(name, "name");
        this.name_ = name;
        return this;
    }

    /**
     * <p>Executes start operations for this instance.</p>
     * <p>This method is called by {@link #start()}.</p>
     */
    protected abstract void onStart();

    /**
     * <p>Executes stop operations for this instance.</p>
     * <p>This method is called by {@link #stop()}.</p>
     */
    protected abstract void onStop();
}
