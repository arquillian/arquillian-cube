package org.arquillian.cube.docker.impl.client.reporter;

import org.arquillian.cube.docker.drone.event.AfterVideoRecorded;
import org.arquillian.extension.recorder.When;
import org.arquillian.recorder.reporter.ReporterConfiguration;
import org.arquillian.recorder.reporter.event.PropertyReportEvent;
import org.arquillian.recorder.reporter.model.entry.VideoEntry;
import org.jboss.arquillian.core.api.Event;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;

import java.nio.file.Path;
import java.nio.file.Paths;

public class TakeVncDroneVideo {

    @Inject
    Event<PropertyReportEvent> propertyReportEvent;

    // Executes after drone recording has finished and file is generated
    public void reportScreencastRecording(@Observes AfterVideoRecorded event, ReporterConfiguration reporterConfiguration) {

        final Path videoLocation = event.getVideoLocation();
        if (videoLocation != null) {

            VideoEntry videoEntry = new VideoEntry();
            videoEntry.setType("x-flv");
            videoEntry.setPhase(When.IN_TEST);

            final Path rootDir = Paths.get(reporterConfiguration.getRootDir().getName());
            final Path relativize = rootDir.relativize(videoLocation);

            videoEntry.setPath(relativize.toString());
            videoEntry.setLink(relativize.toString());

            propertyReportEvent.fire(new PropertyReportEvent(videoEntry));

        }
    }

}
