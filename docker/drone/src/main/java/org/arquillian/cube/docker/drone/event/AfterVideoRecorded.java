package org.arquillian.cube.docker.drone.event;

import org.jboss.arquillian.core.spi.event.Event;

import java.nio.file.Path;

public class AfterVideoRecorded implements Event {

    private Path videoLocation;

    public AfterVideoRecorded(Path videoLocation) {
        this.videoLocation = videoLocation;
    }

    public Path getVideoLocation() {
        return videoLocation;
    }
}
