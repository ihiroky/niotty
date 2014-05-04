package net.ihiroky.niotty;

/**
 * The factory to create the instance of {@link net.ihiroky.niotty.EventDispatcher}.
 * @param <E> The type of {@link net.ihiroky.niotty.EventDispatcher}
 */
public interface EventDispatcherFactory<E extends EventDispatcher> {

    /**
     * Creates a new event dispatcher.
     *
     * @return the event dispatcher
     */
    E newEventDispatcher();
}
