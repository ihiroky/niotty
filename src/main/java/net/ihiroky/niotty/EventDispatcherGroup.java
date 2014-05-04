package net.ihiroky.niotty;

import net.ihiroky.niotty.util.Arguments;
import net.ihiroky.niotty.util.Closable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ThreadFactory;

/**
 * Provides a thread pool to execute {@link EventDispatcher} and manages the event dispatcher lifecycle.
 *
 * @param <E> the actual type of the EventDispatcher
 */
public class EventDispatcherGroup<E extends EventDispatcher> implements Closable {

    private final Collection<E> eventDispatchers_;
    private final ThreadFactory threadFactory_;
    private final EventDispatcherFactory<E> eventDispatcherFactory_;
    private final int workers_;
    private Logger logger_ = LoggerFactory.getLogger(EventDispatcherGroup.class);

    /**
     * Constructs a new instance.
     *
     * @param workers the number of threads held in the thread pool
     * @param threadFactory a factory to create thread which runs a event dispatcher
     * @param eventDispatcherFactory a factory to create the instance of {@link net.ihiroky.niotty.EventDispatcher}
     */
    public EventDispatcherGroup(int workers, ThreadFactory threadFactory,
            EventDispatcherFactory<E> eventDispatcherFactory) {
        eventDispatchers_ = new HashSet<E>();
        threadFactory_ = Arguments.requireNonNull(threadFactory, "threadFactory");
        workers_ = Arguments.requirePositive(workers, "workers");
        eventDispatcherFactory_ = Arguments.requireNonNull(eventDispatcherFactory, "eventDispatcherFactory");
    }

    /**
     * Cleans up and Sets up the thread pool and if not created.
     * This method may as well bing called ahead.
     */
    public final void open() {
        List<E> newEventDispatcherList;
        synchronized (eventDispatchers_) {
            for (Iterator<E> iterator = eventDispatchers_.iterator(); iterator.hasNext();) {
                E eventDispatcher = iterator.next();
                if (!eventDispatcher.isAlive()) {
                    logger_.debug("[open0] Dead event dispatcher {} is found. Remove it and assign a new one.", eventDispatcher);
                    iterator.remove();
                }
            }

            int n = workers_ - eventDispatchers_.size();
            if (n == 0) {
                return;
            }
            newEventDispatcherList = new ArrayList<E>(n);
            for (int i = 0; i < n; i++) {
                E newEventDispatcher = eventDispatcherFactory_.newEventDispatcher();
                Thread thread = threadFactory_.newThread(newEventDispatcher);
                thread.start();
                eventDispatchers_.add(newEventDispatcher);
                newEventDispatcherList.add(newEventDispatcher);
                logger_.debug("[open0] New event dispatcher {} is created. Event dispatcher count: {}.",
                        newEventDispatcher, eventDispatchers_.size());
            }
        }

        // Ensure that dispatcher.onOpen() is called now.
        // And doesn't wait in the synchronized block.
        try {
            for (E newEventDispatcher : newEventDispatcherList) {
                newEventDispatcher.waitUntilStarted();
            }
        } catch (InterruptedException ie) {
            logger_.debug("[open0] Interrupted. Close active event dispatchers.", ie);
            close();
        }
    }

    /**
     * Stops the event dispatchers and releases the thread pool.
     */
    public void close() {
        synchronized (eventDispatchers_) {
            for (E eventDispatcher : eventDispatchers_) {
                eventDispatcher.close();
            }
            eventDispatchers_.clear();
        }
    }

    /**
     * Returns true if the thread pool is available.
     * @return true if the thread pool is available
     */
    public boolean isOpen() {
        synchronized (eventDispatchers_) {
            return !eventDispatchers_.isEmpty();
        }
    }

    /**
     * Offers a event for each event dispatcher.
     * @param event the event to be executed in the event dispatchers
     */
    public void offerEvent(Event event) {
        synchronized (eventDispatchers_) {
            for (E dispatcher : eventDispatchers_) {
                dispatcher.offer(event);
            }
        }
    }

    /**
     * Assigns a dispatcher which weight is under the threshold even if a specified selection is added,
     * or a minimum among the event dispatchers.
     * @param selection the selection added to a selected event dispatcher
     * @return the event dispatcher
     */
    public E assign(EventDispatcherSelection selection) {
        Arguments.requireNonNull(selection, "selection");

        int minCount = Integer.MAX_VALUE;
        E minCountDispatcher = null;
        open();
        synchronized (eventDispatchers_) {
            for (E dispatcher : eventDispatchers_) {
                if (dispatcher.countUpDuplication(selection)) {
                    logger_.debug("[assign] [{}] is already assigned to [{}]", selection, dispatcher);
                    return dispatcher;
                }
                int count = dispatcher.selectionCount();
                if (count < minCount) {
                    minCount = count;
                    minCountDispatcher = dispatcher;
                }
            }
        }
        if (minCountDispatcher == null) {
            throw new IllegalStateException("This EventDispatcherGroup may be closed.");
        }
        minCountDispatcher.accept(selection);
        logger_.debug("[assign] {} is assigned to {}", selection, minCountDispatcher);
        return minCountDispatcher;
    }
}
