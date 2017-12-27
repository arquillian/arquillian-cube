package org.arquillian.cube.impl.util;

import org.arquillian.cube.docker.impl.requirement.RequiresDocker;
import org.arquillian.cube.docker.impl.requirement.RequiresDockerMachine;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@Category({RequiresDocker.class, RequiresDockerMachine.class})
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
