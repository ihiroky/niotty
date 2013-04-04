package net.ihiroky.niotty;

import java.util.Objects;

/**
 * @author Hiroki Itoh
 */
public class StageKeys {

    public static StageKey of(int key) {
        return new IntStageKey(key);
    }

    public static StageKey of(String key) {
        return new StringStageKey(key);
    }

    private static class IntStageKey implements StageKey {

        int key_;

        IntStageKey(int key) {
            key_ = key;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof IntStageKey)) {
                return false;
            }
            IntStageKey that = (IntStageKey) obj;
            return this.key_ == that.key_;
        }

        @Override
        public int hashCode() {
            return key_;
        }

        @Override
        public String toString() {
            return IntStageKey.class.getSimpleName() + ':' + key_;
        }
    }

    private static class StringStageKey implements StageKey {

        String key_;

        StringStageKey(String key) {
            Objects.requireNonNull(key, "key");
            key_ = key;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof StringStageKey)) {
                return false;
            }
            StringStageKey that = (StringStageKey) obj;
            return this.key_.equals(that.key_);
        }

        @Override
        public int hashCode() {
            return key_.hashCode();
        }

        @Override
        public String toString() {
            return StringStageKey.class.getSimpleName() + ':' + key_;
        }
    }
}
