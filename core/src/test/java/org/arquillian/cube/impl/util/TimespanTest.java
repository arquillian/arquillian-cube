package org.arquillian.cube.impl.util;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class TimespanTest {


    @Test
    public void should_transform_from_milliseconds_string_to_milleseconds() {
        final long milliseconds = Timespan.toMilliseconds("250ms");
        assertThat(milliseconds, is(250L));
    }

    @Test
    public void should_transform_from_seconds_and_minutes_string_to_milliseconds() {
        final long milliseconds = Timespan.toMilliseconds("1m30s");
        assertThat(milliseconds, is(90000L));
    }

}
