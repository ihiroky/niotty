package net.ihiroky.niotty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadFactory;

/**
 * Created on 13/01/10, 18:43
 *
 * @author Hiroki Itoh
 */
public abstract class EventLoopGroup<L extends EventLoop<L>> {

    private Collection<L> eventLoops = nullValue();
    private ContextTransportAggregate contextTransportAggregate;
    private Logger logger = LoggerFactory.getLogger(EventLoopGroup.class);

    public synchronized void open(ThreadFactory threadFactory, int numberOfWorker) {
        if (!isInitialized(eventLoops())) {
            L[] loops = newArray(numberOfWorker);
            for (int i = 0; i < numberOfWorker; i++) {
                L loop = newEventLoop();
                loop.openWith(threadFactory, null, null);
                loops[i] = loop;
                logger.info("start event loop {}", loop);
            }
            eventLoops = Collections.unmodifiableCollection(new CopyOnWriteArrayList<L>(loops));
        }
    }

    public synchronized void open(ThreadFactory threadFactory,
                                  int numberOfWorker,
                                  PipeLineFactory pipeLineFactory,
                                  StageContextListener<?> storeListener){
        if (!isInitialized(eventLoops())) {
            L[] loops = newArray(numberOfWorker);
            pipeLineFactory.createStorePipeLine();
            ContextTransportAggregate transportAggregate =
                    new ContextTransportAggregate(pipeLineFactory.createStorePipeLine());
            for (int i = 0; i < numberOfWorker; i++) {
                PipeLine loadPipeLine = pipeLineFactory.createLoadPipeLine(transportAggregate);
                PipeLine storePipeLine = pipeLineFactory.createStorePipeLine();
                storePipeLine.getLastContext().addListener(storeListener);
                L loop = newEventLoop();
                loop.openWith(threadFactory, loadPipeLine, storePipeLine);
                loops[i] = loop;
                logger.info("start event loop {}.", loop);
            }
            contextTransportAggregate = transportAggregate;
            eventLoops = Collections.unmodifiableCollection(new CopyOnWriteArrayList<L>(loops));
        }
    }

    @SuppressWarnings("unchecked")
    public synchronized void close() {
        if (isInitialized(eventLoops())) {
            for (L loop : eventLoops) {
                loop.close();
            }
            eventLoops = nullValue();
        }
    }

    public void offerTask(EventLoop.Task<L> task) {
        for (L loop : eventLoops) {
            loop.offerTask(task);
        }
    }

    protected Collection<L> eventLoops() {
        return eventLoops;
    }

    abstract protected L newEventLoop();

    private static Collection<EventLoop<?>> NULL = Collections.emptyList();

    private <L> boolean isInitialized(Collection<L> c) {
        return c != NULL;
    }

    @SuppressWarnings("unchecked")
    private Collection<L> nullValue() {
        return (Collection<L>) NULL;
    }

    @SuppressWarnings("unchecked")
    private static <L extends EventLoop<L>> L[] newArray(int size) {
        return (L[]) new EventLoop<?>[size];
    }

    public ContextTransportAggregate getContextTransportAggregate() {
        return contextTransportAggregate;
    }
}
