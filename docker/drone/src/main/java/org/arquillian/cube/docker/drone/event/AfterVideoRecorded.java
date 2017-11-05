package org.arquillian.cube.docker.drone.event;

import java.nio.file.Path;
import org.jboss.arquillian.core.spi.event.Event;
import org.jboss.arquillian.test.spi.event.suite.After;

public class AfterVideoRecorded implements Event {

    private Path videoLocation;
    private After after;

    public AfterVideoRecorded(After after, Path videoLocation) {
        this.after = after;
        this.videoLocation = videoLocation;
    }

    public Path getVideoLocation() {
        return videoLocation;
    }

    public After getAfter() {
        return after;
    }
}
