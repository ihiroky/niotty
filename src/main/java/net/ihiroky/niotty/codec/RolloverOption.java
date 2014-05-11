package net.ihiroky.niotty.codec;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Provides options to control rollover operation of {@link net.ihiroky.niotty.codec.PacketWriter}.
 * The size rollover and the time (daily) rollovers are supported.
 */
public class RolloverOption {

    private final long rolloverSize_;
    private final List<Time> rolloverTimeList_;

    private long size_;
    private long nextRolloverTime_;

    /**
     * Constructs a new instance.
     *
     * An invocation of this constructor behaves in exactly the same way as the invocation
     * {@code RolloverOption(rolloverSize, Collections.<Time>emptySet())}.
     *
     * @param rolloverSize the size to roll over
     */
    public RolloverOption(long rolloverSize) {
        this(rolloverSize, Collections.<Time>emptySet());
    }

    /**
     * Constructs a new instance.
     *
     * An invocation of this constructor behaves in exactly the same way as the invocation
     * {@code RolloverOption(Long.MAX_VALUE, rolloverTimeSet)}.
     *
     * @param rolloverTimeSet the times to roll over
     */
    public RolloverOption(Set<Time> rolloverTimeSet) {
        this(Long.MAX_VALUE, rolloverTimeSet);
    }

    /**
     * Constructs a new instance.
     *
     * @param rolloverSize the size to roll over
     * @param rolloverTimeSet the times to roll over
     */
    public RolloverOption(long rolloverSize, Set<Time> rolloverTimeSet) {
        this(rolloverSize, rolloverTimeSet, System.currentTimeMillis());
    }

    RolloverOption(long rolloverSize, Set<Time> rolloverTimeSet, long now) {
        rolloverSize_ = rolloverSize;
        if (rolloverTimeSet != null) {
            rolloverTimeList_ = Collections.unmodifiableList(new ArrayList<Time>(rolloverTimeSet));
            nextRolloverTime_ = searchMinimumRolloverTime(rolloverTimeList_, now);
        } else {
            rolloverTimeList_ = Collections.emptyList();
        }

    }

    private static long searchMinimumRolloverTime(List<Time> rolloverTimeList, long now) {
        long min = Long.MAX_VALUE;
        for (Time time : rolloverTimeList) {
            long rolloverTime = time.timeInMillis(now);
            if (rolloverTime < min) {
                min = rolloverTime;
            }
        }
        return min;
    }

    /**
     * Returns the rollover size.
     * @return the rollover size
     */
    public long rolloverSize() {
        return rolloverSize_;
    }

    /**
     * Returns the rollover times
     * @return the rollover times
     */
    public Collection<Time> rolloverTimes() {
        return rolloverTimeList_;
    }

    public long nexRolloverTime() {
        return nextRolloverTime_;
    }

    boolean requiresRollover(int packetSize, long now) {
        boolean requiresRollover = false;
        size_ += packetSize;
        if (size_ > rolloverSize_) {
            size_ = packetSize;
            requiresRollover = true;
        }
        if (now >= nextRolloverTime_) {
            nextRolloverTime_ = searchMinimumRolloverTime(rolloverTimeList_, now);
            requiresRollover = true;
        }
        return requiresRollover;
    }

    long size() {
        return size_;
    }

    /**
     * The representation of HH:mm:ss for the rollover time.
     */
    public static class Time implements Comparable<Time> {
        final int hour_;
        final int minute_;
        final int second_;

        private Calendar calendar_;

        private static final String SEPARATOR = ":";

        /**
         * Parse the string arguments as a set of {@code Time}.
         * The format of the string is {@code "HH:mm.ss"}.
         *
         * @param hms the string arguments
         * @return the set of {@code Time}
         */
        public static Set<Time> prase(String[] hms) {
            Set<Time> timeSet = new TreeSet<Time>();
            for (String s : hms) {
                timeSet.add(parse(s));
            }
            return timeSet;
        }

        /**
         * Parse the string argument as a {@code Time}.
         * The format of the string is {@code "HH:mm.ss"}.
         *
         * @param hms the string argument
         * @return the {@code Time}
         */
        public static Time parse(String hms) {
            if (hms == null) {
                throw new NullPointerException("hms");
            }
            String[] s = hms.split(SEPARATOR);
            if (s.length != 3) {
                throw new IllegalArgumentException("HH:MM:SS is required.");
            }
            int hour, minute, second;
            try {
                hour = Integer.parseInt(s[0]);
            } catch (NumberFormatException nfe) {
                throw new IllegalArgumentException("Invalid hour: " + s[0], nfe);
            }
            try {
                minute = Integer.parseInt(s[1]);
            } catch (NumberFormatException nfe) {
                throw new IllegalArgumentException("Invalid minute: " + s[1], nfe);
            }
            try {
                second = Integer.parseInt(s[2]);
            } catch (NumberFormatException nfe) {
                throw new IllegalArgumentException("Invalid second: " + s[2], nfe);
            }

            return new Time(hour, minute, second);
        }

        /**
         * Constructs a new instance
         * @param hour the hour
         * @param minute the minute
         * @param second the second
         */
        public Time(int hour, int minute, int second) {
            if (hour < 0 || hour > 23) {
                throw new IllegalArgumentException("The hour requires in range [0, 23].");
            }
            if (minute < 0 || minute > 59) {
                throw new IllegalArgumentException("The minute requires in range [0, 59].");
            }
            if (second < 0 || second > 59) {
                throw new IllegalArgumentException("The second requires in range [0, 59].");
            }
            hour_ = hour;
            minute_ = minute;
            second_ = second;
            calendar_ = Calendar.getInstance();
        }

        long timeInMillis(long now) {
            Calendar c = calendar_;
            c.set(Calendar.HOUR_OF_DAY, hour_);
            c.set(Calendar.MINUTE, minute_);
            c.set(Calendar.SECOND, second_);
            c.set(Calendar.MILLISECOND, 0);
            long nextRolloverTime = c.getTimeInMillis();
            if (nextRolloverTime > now) {
                return nextRolloverTime;
            }
            c.add(Calendar.DAY_OF_MONTH, 1);
            return c.getTimeInMillis();
        }

        @Override
        public boolean equals(Object object) {
            if (object instanceof Time) {
                Time that = (Time) object;
                return this.hour_ == that.hour_
                        && this.minute_ == that.minute_
                        && this.second_ == that.second_;
            }
            return false;
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(new Object[]{hour_, minute_, second_});
        }

        @Override
        public int compareTo(Time o) {
            int c = hour_ - o.hour_;
            if (c != 0) {
                return c;
            }
            c = minute_ - o.minute_;
            if (c != 0) {
                return c;
            }
            return second_ - o.second_;
        }

        @Override
        public String toString() {
            return String.format("%02d:%02d:%02d", hour_, minute_, second_);
        }
    }
}
