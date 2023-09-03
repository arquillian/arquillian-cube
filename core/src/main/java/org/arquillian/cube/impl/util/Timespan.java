package org.arquillian.cube.impl.util;/*
 * Copyright 2010-2010 LinkedIn, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

import java.util.Arrays;
import java.util.Collections;

import java.io.Serializable;
import java.util.Date;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;

/**
 * This class represents a notion of timespan. Note that the time units goes from milliseconds to year.
 * It does not go below milliseconds because we rarely use this kind of precision especially since the
 * java vm does not support it very well. Note that above hour, the timespan is approximate. This
 * object is immutable and thread safe.
 *
 * @author ypujante@linkedin.com
 */
public class Timespan implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum TimeUnit {
        MILLISECOND(1L, ""),
        SECOND(1000L * MILLISECOND.getMillisecondsCount(), "s"),
        MINUTE(60L * SECOND.getMillisecondsCount(), "m"),
        HOUR(60L * MINUTE.getMillisecondsCount(), "h"),
        // note that the values below are approximations, and should not be used to compute exact
        // values!
        DAY(24L * HOUR.getMillisecondsCount(), "d"),
        WEEK(7L * DAY.getMillisecondsCount(), "w"),
        MONTH(30L * DAY.getMillisecondsCount(), "M"),
        YEAR(365L * DAY.getMillisecondsCount(), "y");

        private final long _millisecondsCount;
        private final String _displayChar;

        private TimeUnit(long millisecondsCount, String displayChar) {
            _millisecondsCount = millisecondsCount;
            _displayChar = displayChar;
        }

        public long getMillisecondsCount() {
            return _millisecondsCount;
        }

        public String getDisplayChar() {
            return _displayChar;
        }
    }

    public final static Timespan ZERO_YEARS = new Timespan(0, TimeUnit.YEAR);
    public final static Timespan ZERO_MONTHS = new Timespan(0, TimeUnit.MONTH);
    public final static Timespan ZERO_WEEKS = new Timespan(0, TimeUnit.WEEK);
    public final static Timespan ZERO_DAYS = new Timespan(0, TimeUnit.DAY);
    public final static Timespan ZERO_HOURS = new Timespan(0, TimeUnit.HOUR);
    public final static Timespan ZERO_MINUTES = new Timespan(0, TimeUnit.MINUTE);
    public final static Timespan ZERO_SECONDS = new Timespan(0, TimeUnit.SECOND);
    public final static Timespan ZERO_MILLISECONDS = new Timespan(0, TimeUnit.MILLISECOND);

    public final static Timespan ONE_SECOND = new Timespan(1, TimeUnit.SECOND);
    public final static Timespan ONE_MINUTE = new Timespan(1, TimeUnit.MINUTE);

    private final static EnumMap<TimeUnit, Timespan> ZERO_TIMESPANS =
        new EnumMap<>(TimeUnit.class);

    private final static TimeUnit[] TIME_UNIT_ORDER;

    static {
        final List<TimeUnit> timeUnits = Arrays.asList(TimeUnit.values());
        Collections.reverse(timeUnits);

        TIME_UNIT_ORDER = timeUnits.toArray(new TimeUnit[0]);
    }

    static {
        ZERO_TIMESPANS.put(TimeUnit.YEAR, ZERO_YEARS);
        ZERO_TIMESPANS.put(TimeUnit.MONTH, ZERO_MONTHS);
        ZERO_TIMESPANS.put(TimeUnit.WEEK, ZERO_WEEKS);
        ZERO_TIMESPANS.put(TimeUnit.DAY, ZERO_DAYS);
        ZERO_TIMESPANS.put(TimeUnit.HOUR, ZERO_HOURS);
        ZERO_TIMESPANS.put(TimeUnit.MINUTE, ZERO_MINUTES);
        ZERO_TIMESPANS.put(TimeUnit.SECOND, ZERO_SECONDS);
        ZERO_TIMESPANS.put(TimeUnit.MILLISECOND, ZERO_MILLISECONDS);
    }

    private final static EnumSet<TimeUnit> CANONICAL_TIME_UNITS =
        EnumSet.range(TimeUnit.MILLISECOND, TimeUnit.YEAR);

    private final long _duration;
    private final TimeUnit _timeUnit;

    /**
     * Constructor
     *
     * @param durationInMilliseconds
     *     the duration in milliseconds
     */
    public Timespan(long durationInMilliseconds) {
        this(durationInMilliseconds, TimeUnit.MILLISECOND);
    }

    /**
     * Constructor
     */
    public Timespan(long duration, TimeUnit timeUnit) {
        _duration = duration;
        _timeUnit = timeUnit;
    }

    /**
     * @return the time unit of this timespan
     */
    public TimeUnit getTimeUnit() {
        return _timeUnit;
    }

    /**
     * @return the duration of this timespan
     */
    public long getDuration() {
        return _duration;
    }

    /**
     * Adds another timespan to this timespan and return a brand new one. Note that the unit is
     * preserved if <code>other</code> has the same unit as 'this'.
     *
     * @param other
     *     the timespan to add
     *
     * @return a brand new timespan.
     */
    public Timespan add(Timespan other) {
        if (getTimeUnit() == other.getTimeUnit()) {
            return new Timespan(getDuration() + other.getDuration(), getTimeUnit());
        }

        return new Timespan(getDurationInMilliseconds() + other.getDurationInMilliseconds(),
            TimeUnit.MILLISECOND);
    }

    /**
     * Creates and returns a new timespan whose duration is {@code this}
     * timespan's duration minus the {@code other} timespan's duration.
     * <p>
     * The time unit is preserved if {@code other} has the same unit
     * as {@code this}.
     * <p>
     * Negative timespans are not supported, so if the {@code other}
     * timespan duration is greater than {@code this} timespan duration,
     * a timespan of zero is returned (i.e., a negative timespan is never
     * returned).
     *
     * @param other
     *     the timespan to subtract from this one
     *
     * @return a new timespan representing {@code this - other}
     */
    public Timespan substractWithZeroFloor(Timespan other) {
        if (getTimeUnit() == other.getTimeUnit()) {
            long delta = Math.max(0, getDuration() - other.getDuration());
            return new Timespan(delta, getTimeUnit());
        }

        long delta = Math.max(0, getDurationInMilliseconds() - other.getDurationInMilliseconds());

        return new Timespan(delta, TimeUnit.MILLISECOND);
    }

    /**
     * @return the duration of this timespan in milliseconds
     */
    public long getDurationInMilliseconds() {
        // 100% equivalent to getDuration(TimeUnit.MILLISECOND) but faster!
        return getDuration() * getTimeUnit().getMillisecondsCount();
    }

    /**
     * @param timeUnit
     *     the unit of time you want this timespan ass
     *
     * @return the duration of this timespan expressed in the time unit provided. Note that all
     * units below timeUnit will be truncated! (ex: 3h20m45s will return 3 if timeUnit=HOUR).
     */
    public long getDuration(TimeUnit timeUnit) {
        return truncate(timeUnit).getDuration();
    }

    /**
     * @return the duration of this timespan expressed in seconds. If milliseconds are present,
     * they will be truncated!
     */
    public long getDurationInSeconds() {
        return getDuration(TimeUnit.SECOND);
    }

    /**
     * @return the duration of this timespan expressed in minutes. If s/ms are present,
     * they will be truncated!
     */
    public long getDurationInMinutes() {
        return getDuration(TimeUnit.MINUTE);
    }

    /**
     * @return the duration of this timespan expressed in hours. If m/s/ms are present,
     * they will be truncated!
     */
    public long getDurationInHours() {
        return getDuration(TimeUnit.HOUR);
    }

    /**
     * @return a (potentially new) version of this timestamp where the unit is
     * {@link TimeUnit#MILLISECOND}.
     */
    public Timespan toMillisecondsTimespan() {
        if (getTimeUnit() == TimeUnit.MILLISECOND) {
            return this;
        }

        return new Timespan(getDurationInMilliseconds(), TimeUnit.MILLISECOND);
    }

    /**
     * Truncates this timespan to the given time unit. Example: if this is 1h20m5s then
     * <code>truncate(TimeUnit.HOUR)</code> will return 1h,
     * <code>truncate(TimeUnit.MINUTE)</code> will return 1h20m and
     * <code>truncate(TimeUnit.SECOND)</code> will return 1h20m5s
     *
     * @param timeUnit
     *     the unit you want the timespan in
     *
     * @return this timespan if time unit matches otherwise a new one with the given unit
     */
    public Timespan truncate(TimeUnit timeUnit) {
        if (getTimeUnit() == timeUnit) {
            return this;
        }

        return truncateDurationToUnit(getDurationInMilliseconds(), timeUnit);
    }

    /**
     * There are many ways to represent the same timespan: 3h, 180m... this call return the unique
     * way to express it such that ms is &lt; 1000, s is &lt; 60, m is &lt; 60, h is &lt; 24,
     * d is &lt; 7.
     *
     * @return this timespan as a canonical representation: an entry for w/dh/m/s/ms is returned
     *
     * @see #getAsTimespans(EnumSet)
     */
    public EnumMap<TimeUnit, Timespan> getCanonicalTimespans() {
        return getAsTimespans(CANONICAL_TIME_UNITS);
    }

    /**
     * @return a string representing this timespan (ex: 3h2m23s). Note that if a duration is missing,
     * it is not part of the string (the string would be 3h23s and not 3h0m23s).
     *
     * @see #getCanonicalTimespans()
     */
    public String getCanonicalString() {
        return getAsString(CANONICAL_TIME_UNITS);
    }

    /**
     * Decomposes this timespan as a map for each unit provided. Example: if this timespan represents
     * 63s and you provide m/s/ms then you will get 3 timespans in the map: one of 1mn, one for 3s
     * and one for 0ms.
     *
     * @param timeUnits
     *     the time units you want to be part of the decomposition.
     *
     * @return a map containing an entry for each time unit provided. All others are <code>null</code>.
     */
    public EnumMap<TimeUnit, Timespan> getAsTimespans(EnumSet<TimeUnit> timeUnits) {
        EnumMap<TimeUnit, Timespan> res = new EnumMap<TimeUnit, Timespan>(TimeUnit.class);

        long durationInMillis = getDurationInMilliseconds();

        for (TimeUnit timeUnit : TIME_UNIT_ORDER) {
            if (timeUnits.contains(timeUnit)) {
                Timespan timespan = truncateDurationToUnit(durationInMillis, timeUnit);
                res.put(timeUnit, timespan);
                durationInMillis -= timespan.getDurationInMilliseconds();
            }
        }

        return res;
    }

    /**
     * Filters this timespan with only the unit provided. It computes the canonical representation
     * and keeps only the units that are present in the filter. Example: if this timespan represents
     * 3d2h25m10s and filter is d/h/m then result is 3d2h25m.
     *
     * @param timeUnits
     *     the time units you want to keep in the filtering.
     *
     * @return the timespan after the filtering
     */
    public Timespan filter(EnumSet<TimeUnit> timeUnits) {
        Timespan res = null;

        EnumMap<TimeUnit, Timespan> canonicalTimespans = getCanonicalTimespans();

        for (TimeUnit timeUnit : TIME_UNIT_ORDER) {
            if (timeUnits.contains(timeUnit)) {
                Timespan timespan = canonicalTimespans.get(timeUnit);
                if (timespan != null && timespan.getDuration() > 0) {
                    if (res == null) {
                        res = timespan;
                    } else {
                        res = res.add(timespan);
                    }
                }
            }
        }

        // in case there is no match we return with smallest timeunit provided
        if (res == null) {
            res = ZERO_TIMESPANS.get(timeUnits.iterator().next());
        }

        return res;
    }

    /**
     * Returns a string representing this timespan expressed with the units provided.
     *
     * @param timeUnits
     *     the timeunits you want in the decomposition
     *
     * @return a string representation using the units.
     *
     * @see #getAsTimespans(EnumSet)
     */
    public String getAsString(EnumSet<TimeUnit> timeUnits) {
        StringBuilder sb = new StringBuilder();

        EnumMap<TimeUnit, Timespan> canonicalTimespans = getAsTimespans(timeUnits);
        for (TimeUnit timeUnit : TIME_UNIT_ORDER) {
            if (canonicalTimespans.containsKey(timeUnit)) {
                long duration = canonicalTimespans.get(timeUnit).getDuration();
                if (duration > 0) {
                    sb.append(duration).append(timeUnit.getDisplayChar());
                }
            }
        }

        if (sb.length() == 0) {
            sb.append(0);
            if (timeUnits.contains(getTimeUnit())) {
                sb.append(getTimeUnit().getDisplayChar());
            }
        }

        return sb.toString();
    }

    /**
     * 2 timespans can be different (1h and 3600s) while representing the same duration expressed
     * in milliseconds... This method tests for this.
     *
     * @param timespan
     *     the other timespan to compare with
     *
     * @return <code>true</code> if this timespan and the provided one represent the same
     * duration of time.
     */
    public boolean equalsDurationInMilliseconds(Timespan timespan) {
        // shortcut when time unit are the same
        if (timespan.getTimeUnit() == getTimeUnit()) {
            return timespan.getDuration() == getDuration();
        } else {
            return getDurationInMilliseconds() == timespan.getDurationInMilliseconds();
        }
    }

    /**
     * @param baseMilliseconds
     *     the starting point to compute the future time
     *
     * @return the absolute time in milliseconds (since jan 01 1970) represented
     * {@code baseMilliseconds} + this timespan
     */
    public long futureTimeMillis(long baseMilliseconds) {
        return baseMilliseconds + getDurationInMilliseconds();
    }

    /**
     * @param baseMillis
     *     the starting point
     *
     * @return baseMillis - this timespan
     */
    public long pastTimeMillis(long baseMillis) {
        return baseMillis - getDurationInMilliseconds();
    }

    /**
     * @param baseDate
     *     base date to offset from
     *
     * @return return the date which is baseDate + value of this timespan
     */
    public Date futureDate(Date baseDate) {
        return new Date(baseDate.getTime() + getDurationInMilliseconds());
    }

    /**
     * @param baseDate
     *     base date to offset from
     *
     * @return return the date which is baseDate - value of this timespan
     */
    public Date pastDate(Date baseDate) {
        return new Date(pastTimeMillis(baseDate.getTime()));
    }

    /**
     * Expresses the provided duration in the unit provided. Note that the timespan returned
     * represent only the truncated version of the duration: if duration is 1002ms and timeunit
     * is seconds, then the timespan returned is 1 second... leaving behind 2ms.
     *
     * @return the timespan
     */
    private static Timespan truncateDurationToUnit(long durationInMillis, TimeUnit timeUnit) {
        Timespan res;

        if (durationInMillis >= timeUnit.getMillisecondsCount()) {
            res = new Timespan(durationInMillis / timeUnit.getMillisecondsCount(),
                timeUnit);
        } else {
            res = ZERO_TIMESPANS.get(timeUnit);
        }

        return res;
    }

    /**
     * @return a string representation of the object.
     */
    @Override
    public String toString() {
        return getCanonicalString();
    }

    /**
     * Creates a timespan from a list of other timespans.
     *
     * @return a timespan representing the sum of all the timespans provided
     */
    public static Timespan create(Timespan... timespans) {
        if (timespans == null) {
            return null;
        }

        if (timespans.length == 0) {
            return ZERO_MILLISECONDS;
        }

        Timespan res = timespans[0];

        for (int i = 1; i < timespans.length; i++) {
            Timespan timespan = timespans[i];
            res = res.add(timespan);
        }

        return res;
    }

    /**
     * Synonym.
     *
     * @see #parseTimespan(String)
     */
    public static Timespan parse(String timespan) {
        return parseTimespan(timespan);
    }

    /**
     * Synonym.
     *
     * @see #parseTimespan(String)
     */
    public static Timespan valueOf(String timespan) {
        return parseTimespan(timespan);
    }

    /**
     * Convenient call when providing milliseconds
     *
     * @return the timespan
     */
    public static Timespan milliseconds(long milliseconds) {
        return new Timespan(milliseconds);
    }

    /**
     * Convenient call when providing seconds
     *
     * @return the timespan
     */
    public static Timespan seconds(long seconds) {
        return new Timespan(seconds, TimeUnit.SECOND);
    }

    /**
     * Convenient call when providing minutes
     *
     * @return the timespan
     */
    public static Timespan minutes(long minutes) {
        return new Timespan(minutes, TimeUnit.MINUTE);
    }

    /**
     * Parses the provided string as a timespan. It should follow the pattern returned by
     * {@link #getCanonicalString()}. Example: 10m30s
     *
     * @return the timespan to parse
     *
     * @throws IllegalArgumentException
     *     if the string is not valid
     */
    public static Timespan parseTimespan(String timespan) {
        if (timespan == null) {
            return null;
        }

        // easiest way to be compliant with docker-compose format
        timespan = timespan.replace("ms", "");

        int len = timespan.length();
        if (len == 0) {
            return ZERO_MILLISECONDS;
        }


        int count = 0;
        int timeUnitOrderIdx = 0;
        int timeUnitOrderLen = TIME_UNIT_ORDER.length;
        Timespan[] timespans = new Timespan[timeUnitOrderLen];

        int startDigitsIdx = 0;
        boolean expectingDigits = true;

        for (int i = 0; i < len; i++) {
            char c = timespan.charAt(i);
            if (isDigit(c)) {
                expectingDigits = false;
                continue;
            }

            if (expectingDigits) {
                throw new IllegalArgumentException("found " + c + " was expecting a digit");
            }

            for (; timeUnitOrderIdx < timeUnitOrderLen; timeUnitOrderIdx++) {
                TimeUnit timeUnit = TIME_UNIT_ORDER[timeUnitOrderIdx];
                String displayChar = timeUnit.getDisplayChar();
                if (displayChar.length() == 0) {
                    throw new IllegalArgumentException("found nothing was expecting: " + c);
                }
                if (c == displayChar.charAt(0)) {
                    try {
                        long duration = Long.parseLong(timespan.substring(startDigitsIdx, i));
                        timespans[timeUnitOrderIdx++] = new Timespan(duration, timeUnit);
                        startDigitsIdx = i + 1;
                        expectingDigits = true;
                        count++;
                        break;
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException(e);
                    }
                }
            }
        }

        if (startDigitsIdx < len) {
            try {
                long duration = Long.parseLong(timespan.substring(startDigitsIdx, len));
                timespans[timeUnitOrderLen - 1] = new Timespan(duration, TimeUnit.MILLISECOND);
                count++;
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(e);
            }
        }

        Timespan[] ts = new Timespan[count];
        for (int i = 0, idx = 0; i < timespans.length; i++) {
            Timespan t = timespans[i];
            if (t != null) {
                ts[idx++] = t;
            }
        }
        return create(ts);
    }

    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    /**
     * Shortcut for creating a timespan and then retrieving the value in milliseconds.
     *
     * @param timespan
     *     the timespan as a string
     *
     * @return the value in milliseconds
     */
    public static long toMilliseconds(String timespan) {
        return parseTimespan(timespan).getDurationInMilliseconds();
    }
}
