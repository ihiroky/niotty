package net.ihiroky.niotty.codec;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 *
 */
public class WeightedMessageTest {

    @Test
    public void testEqualsReturnsTrue() throws Exception {
        Object message = new Object();
        WeightedMessage sut = new WeightedMessage(message, 0);
        WeightedMessage wm = new WeightedMessage(message, 0);

        assertThat(sut.equals(wm), is(true));
    }

    @Test
    public void testEqualsReturnsFalseIfWeightIndexIsDifferent() throws Exception {
        Object message = new Object();
        WeightedMessage sut = new WeightedMessage(message, 0);
        WeightedMessage wm = new WeightedMessage(message, 1);

        assertThat(sut.equals(wm), is(false));
    }

    @Test
    public void testEqualsReturnsFalseIfMessageIsDifferent() throws Exception {
        WeightedMessage sut = new WeightedMessage(new Object(), 0);
        WeightedMessage wm = new WeightedMessage(new Object(), 1);

        assertThat(sut.equals(wm), is(false));
    }
}
