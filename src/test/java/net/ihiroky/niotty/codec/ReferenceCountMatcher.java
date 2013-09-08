package net.ihiroky.niotty.codec;

import net.ihiroky.niotty.buffer.ArrayCodecBuffer;
import net.ihiroky.niotty.buffer.CodecBuffer;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;

/**
 *
 */
public class ReferenceCountMatcher extends BaseMatcher<CodecBuffer> {

    int actual_;
    int expected_;

    public static ReferenceCountMatcher hasReferenceCount(int expected) {
        return new ReferenceCountMatcher(expected);
    }

    ReferenceCountMatcher(int expected) {
        expected_ = expected;
    }

    @Override
    public boolean matches(Object item) {
        actual_ = ((ArrayCodecBuffer) item).referenceCount();
        return actual_ == expected_;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("has reference count ").appendValue(expected_);
    }

    @Override
    public void describeMismatch(Object item, Description description) {
        description.appendText("had ").appendValue(actual_);
    }

}
