package net.ihiroky.niotty;

import net.ihiroky.niotty.util.Objects;

import java.util.concurrent.ThreadFactory;

/**
 * @author Hiroki Itoh
 */
public class NameCountThreadFactory implements ThreadFactory {

    private String name_;
    private int count_;

    public NameCountThreadFactory(String name) {
        Objects.requireNonNull(name, "name");
        this.name_ = name.concat(":");
    }

    @Override
    public Thread newThread(Runnable r) {
        return new Thread(r, name_.concat(Integer.toString(count_++)));
    }
}
