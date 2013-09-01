package net.ihiroky.niotty;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * @author Hiroki Itoh
 */
public class DefaultPipelineElementExecutor implements PipelineElementExecutor {

    private TaskLoop taskLoop_;
    private DefaultPipelineElementExecutorPool<?> pool_;

    public DefaultPipelineElementExecutor(TaskLoop taskLoop, DefaultPipelineElementExecutorPool<?> pool) {
        Objects.requireNonNull(taskLoop, "taskLoop");
        taskLoop_ = taskLoop;
        pool_ = pool;
    }

    @Override
    public <I> void execute(final PipelineElement<I, ?> context, final I input) {
        taskLoop_.executeTask(new Task() {
            @Override
            public long execute(TimeUnit timeUnit) throws Exception {
                context.fire(input);
                return TaskLoop.DONE;
            }
        });
    }

    @Override
    public <I> void execute(final PipelineElement<I, ?> context, final I input, final TransportParameter parameter) {
        taskLoop_.executeTask(new Task() {
            @Override
            public long execute(TimeUnit timeUnit) throws Exception {
                context.fire(input, parameter);
                return TaskLoop.DONE;
            }
        });
    }

    @Override
    public void execute(final PipelineElement<?, ?> context, final TransportStateEvent event) {
        taskLoop_.executeTask(new Task() {
            @Override
            public long execute(TimeUnit timeUnit) throws Exception {
                context.fire(event);
                context.proceed(event);
                return TaskLoop.DONE;
            }
        });
    }

    @Override
    public TaskFuture schedule(Task task, long timeout, TimeUnit timeUnit) {
        return taskLoop_.offerTask(task, timeout, timeUnit);
    }

    @Override
    public PipelineElementExecutorPool pool() {
        return pool_;
    }

    @Override
    public void close(PipelineElement<?, ?> context) {
    }
}
