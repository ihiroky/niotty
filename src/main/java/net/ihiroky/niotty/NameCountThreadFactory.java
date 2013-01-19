package net.ihiroky.niotty;

import java.util.Objects;
import java.util.concurrent.ThreadFactory;

/**
 * @author Hiroki Itoh
 */
public class NameCountThreadFactory implements ThreadFactory {

    private String name;
    private int count;

    public NameCountThreadFactory(String name) {
        Objects.requireNonNull(name, "name");
        this.name = name.concat(":");
    }

    @Override
    public Thread newThread(Runnable r) {
        return new Thread(r, name.concat(Integer.toString(count++)));
    }
}
