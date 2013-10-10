package net.ihiroky.niotty;

import java.util.Arrays;

/**
 * A default implementation of {@link net.ihiroky.niotty.TransportParameter}.
 *
 * @author Hiroki Itoh
 */
public class DefaultTransportParameter implements TransportParameter {

    private final int priority_;
    private final Object argument_;

    /** Default priority (no wait). */
    private static final int DEFAULT_PRIORITY = -1;

    public static final DefaultTransportParameter NO_PARAMETER = new DefaultTransportParameter(DEFAULT_PRIORITY);

    /**
     * Creates a new instance.
     * @param argument an argument.
     */
    public DefaultTransportParameter(Object argument) {
        this(DEFAULT_PRIORITY, argument);
    }

    /**
     * Creates a new instance.
     * @param priority a priority to control write operation.
     */
    public DefaultTransportParameter(int priority) {
        this(priority, null);
    }

    /**
     * Creates a new instance.
     * @param priority a priority to control a write operation.
     * @param argument an argument.
     */
    public DefaultTransportParameter(int priority, Object argument) {
        priority_ = priority;
        argument_ = argument;
    }

    @Override
    public int priority() {
        return priority_;
    }

    @Override
    public Object argument() {
        return argument_;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(new Object[]{priority_, argument_});
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof DefaultTransportParameter) {
            DefaultTransportParameter that = (DefaultTransportParameter) object;
            return this.priority_ == that.priority_
                    && ((this.argument_ != null) ? this.argument_.equals(that.argument_) : that.argument_ == null);
        }
        return false;
    }

    @Override
    public String toString() {
        return "argument: " + argument_ + ", priority: " + priority_;
    }
}
