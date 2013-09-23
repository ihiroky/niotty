package net.ihiroky.niotty.util;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

/**
 *
 */
public final class JavaVersionMatchers {

    public static IsGreaterThanOrEqual greaterOrEqual(JavaVersion expected) {
        return new IsGreaterThanOrEqual(expected);
    }

    public static IsEqual equal(JavaVersion expected) {
        return new IsEqual(expected);
    }

    private static class IsEqual extends TypeSafeMatcher<JavaVersion> {

        private JavaVersion expected_;

        private IsEqual(JavaVersion expected) {
            expected_ = expected;
        }

        @Override
        protected boolean matchesSafely(JavaVersion item) {
            return item.equals(expected_);
        }

        @Override
        public void describeTo(Description description) {
            description.appendValue(expected_);
        }
    }

    private static class IsGreaterThanOrEqual extends TypeSafeMatcher<JavaVersion> {

        private JavaVersion expected_;

        private IsGreaterThanOrEqual(JavaVersion expected) {
            expected_ = expected;
        }

        @Override
        protected boolean matchesSafely(JavaVersion item) {
            return item.ge(expected_);
        }

        @Override
        public void describeTo(Description description) {
            description.appendValue(expected_);
        }
    }

    private JavaVersionMatchers() {
        throw new AssertionError();
    }
}
