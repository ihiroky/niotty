package net.ihiroky.niotty;

/**
 * @author Hiroki Itoh
 */
public interface TaskSelection {

    /**
     * Returns the weight of this selection to choose TaskLoop.
     *
     * @return The weight.
     */
    int weight();
}
