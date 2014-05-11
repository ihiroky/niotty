package net.ihiroky.niotty.codec;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

public class RolloverOptionTest {

    @Rule
    public ExpectedException thrownRule_ = ExpectedException.none();

    @Test
    public void testParseTime() throws Exception {
        RolloverOption.Time time = RolloverOption.Time.parse("01:02:03");

        assertThat(time, is(new RolloverOption.Time(1, 2, 3)));
    }

    @Test
    public void testParseTimeInvalidLength() throws Exception {
        thrownRule_.expect(IllegalArgumentException.class);
        thrownRule_.expectMessage("HH:MM:SS is required.");

        RolloverOption.Time.parse("123456789");
    }

    @Test
    public void testParseTimeInvalidHour() throws Exception {
        thrownRule_.expect(IllegalArgumentException.class);
        thrownRule_.expectMessage("Invalid hour: aa");

        RolloverOption.Time.parse("aa:00:00");
    }

    @Test
    public void testParseTimeInvalidMinute() throws Exception {
        thrownRule_.expect(IllegalArgumentException.class);
        thrownRule_.expectMessage("Invalid minute: aa");

        RolloverOption.Time.parse("00:aa:00");
    }

    @Test
    public void testParseTimeInvalidSecond() throws Exception {
        thrownRule_.expect(IllegalArgumentException.class);
        thrownRule_.expectMessage("Invalid second: aa");

        RolloverOption.Time.parse("00:00:aa");
    }

    @Test
    public void testHourIsOutOfRangeMinus1() throws Exception {
        thrownRule_.expect(IllegalArgumentException.class);
        thrownRule_.expectMessage("The hour requires in range [0, 23].");

        new RolloverOption.Time(-1, 0, 0);
    }

    @Test
    public void testHourIsOutOfRange24() throws Exception {
        thrownRule_.expect(IllegalArgumentException.class);
        thrownRule_.expectMessage("The hour requires in range [0, 23].");

        new RolloverOption.Time(24, 0, 0);
    }

    @Test
    public void testMinuteIsOutOfRangeMinus1() throws Exception {
        thrownRule_.expect(IllegalArgumentException.class);
        thrownRule_.expectMessage("The minute requires in range [0, 59].");

        new RolloverOption.Time(0, -1, 0);
    }

    @Test
    public void testMinuiteIsOutOfRange60() throws Exception {
        thrownRule_.expect(IllegalArgumentException.class);
        thrownRule_.expectMessage("The minute requires in range [0, 59].");

        new RolloverOption.Time(0, 60, 0);
    }

    @Test
    public void testSecondIsOutOfRangeMinus1() throws Exception {
        thrownRule_.expect(IllegalArgumentException.class);
        thrownRule_.expectMessage("The second requires in range [0, 59].");

        new RolloverOption.Time(0, 0, -1);
    }

    @Test
    public void testSecondIsOutOfRange60() throws Exception {
        thrownRule_.expect(IllegalArgumentException.class);
        thrownRule_.expectMessage("The second requires in range [0, 59].");

        new RolloverOption.Time(0, 0, 60);
    }

    @Test
    public void testTimeInMillisToday() throws Exception {
        Calendar now = Calendar.getInstance();
        now.set(Calendar.HOUR_OF_DAY, 6);
        now.set(Calendar.MINUTE, 1);
        now.set(Calendar.SECOND, 2);
        now.set(Calendar.MILLISECOND, 0);
        RolloverOption.Time sut = new RolloverOption.Time(6, 1, 3);

        long actual = sut.timeInMillis(now.getTimeInMillis());

        Calendar expected = Calendar.getInstance();
        expected.set(Calendar.HOUR_OF_DAY, 6);
        expected.set(Calendar.MINUTE, 1);
        expected.set(Calendar.SECOND, 3);
        expected.set(Calendar.MILLISECOND, 0);
        assertThat(new Date(actual), is(expected.getTime()));
    }

    @Test
    public void testTimeInMillisTomorrow() throws Exception {
        Calendar now = Calendar.getInstance();
        now.set(Calendar.HOUR_OF_DAY, 6);
        now.set(Calendar.MINUTE, 1);
        now.set(Calendar.SECOND, 2);
        now.set(Calendar.MILLISECOND, 0);
        RolloverOption.Time sut = new RolloverOption.Time(6, 1, 2);

        long actual = sut.timeInMillis(now.getTimeInMillis());

        Calendar expected = Calendar.getInstance();
        expected.set(Calendar.HOUR_OF_DAY, 6);
        expected.set(Calendar.MINUTE, 1);
        expected.set(Calendar.SECOND, 2);
        expected.set(Calendar.MILLISECOND, 0);
        expected.add(Calendar.DAY_OF_MONTH, 1);
        assertThat(new Date(actual), is(expected.getTime()));
    }

    @Test
    public void testRequiresRolloverUnderTheSize() throws Exception {
        RolloverOption sut = new RolloverOption(10, Collections.<RolloverOption.Time>emptySet());

        boolean actual0 = sut.requiresRollover(5, 0L);
        boolean actual1 = sut.requiresRollover(5, 0L);

        assertThat(actual0, is(false));
        assertThat(actual1, is(false));
        assertThat(sut.size(), is(10L));
    }

    @Test
    public void testRequiresRolloverOverTheSize() throws Exception {
        RolloverOption sut = new RolloverOption(10, Collections.<RolloverOption.Time>emptySet());

        boolean actual0 = sut.requiresRollover(5, 0L);
        boolean actual1 = sut.requiresRollover(6, 0L);

        assertThat(actual0, is(false));
        assertThat(actual1, is(true));
        assertThat(sut.size(), is(6L));
    }

    private long timeInMillis(int hours, int minutes, int seconds, int  addDays) {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, hours);
        c.set(Calendar.MINUTE, minutes);
        c.set(Calendar.SECOND, seconds);
        c.set(Calendar.MILLISECOND, 0);
        c.add(Calendar.DAY_OF_MONTH, addDays);
        return c.getTimeInMillis();
    }

    @Test
    public void testRequiresRolloverUnderTheTime() throws Exception {
        long now = timeInMillis(23, 59, 59, 0);
        Set<RolloverOption.Time> times =
                new HashSet<RolloverOption.Time>(Arrays.asList(new RolloverOption.Time(0, 0, 0)));
        RolloverOption sut = new RolloverOption(Long.MAX_VALUE, times, now);

        boolean actual = sut.requiresRollover(0, now);

        assertThat(actual, is(false));
        assertThat(sut.nexRolloverTime(), is(timeInMillis(0, 0, 0, 1)));
    }

    @Test
    public void testRequiresRolloverOverTheTime() throws Exception {
        long now = timeInMillis(23, 59, 59, 0);

        Set<RolloverOption.Time> times =
                new HashSet<RolloverOption.Time>(Arrays.asList(new RolloverOption.Time(0, 0, 0)));
        RolloverOption sut = new RolloverOption(Long.MAX_VALUE, times, now);

        now = timeInMillis(0, 0, 0, 1);
        boolean actual = sut.requiresRollover(0, now);

        assertThat(actual, is(true));
        assertThat(sut.nexRolloverTime(), is(timeInMillis(0, 0, 0, 2)));
    }

    @Test
    public void testRequiresRolloverChoiceNearestTime() throws Exception {
        Set<RolloverOption.Time> times =
                new HashSet<RolloverOption.Time>(Arrays.asList(
                        new RolloverOption.Time(0, 0, 0),
                        new RolloverOption.Time(0, 0, 1),
                        new RolloverOption.Time(0, 0, 2)));
        long now = timeInMillis(23, 59, 59, 0);
        RolloverOption sut = new RolloverOption(Long.MAX_VALUE, times, now);
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 1);

        now = timeInMillis(0, 0, 1, 1);
        boolean actual = sut.requiresRollover(0, now);

        assertThat(actual, is(true));
        assertThat(sut.nexRolloverTime(), is(timeInMillis(0, 0, 2, 1)));
    }
}
