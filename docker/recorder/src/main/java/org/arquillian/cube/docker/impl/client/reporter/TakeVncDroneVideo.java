package org.arquillian.cube.docker.impl.client.reporter;

import org.arquillian.cube.docker.drone.event.AfterVideoRecorded;
import org.arquillian.reporter.api.builder.Reporter;
import org.arquillian.reporter.api.event.SectionEvent;
import org.arquillian.reporter.api.event.TestMethodSection;
import org.arquillian.reporter.api.model.entry.FileEntry;
import org.arquillian.reporter.api.model.report.TestMethodReport;
import org.arquillian.reporter.config.ReporterConfiguration;
import org.jboss.arquillian.core.api.Event;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;

import java.lang.reflect.Method;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TakeVncDroneVideo {

    @Inject
    Event<SectionEvent> reportEvent;

    // Executes after drone recording has finished and file is generated
    public void reportScreencastRecording(@Observes AfterVideoRecorded event, ReporterConfiguration reporterConfiguration) {

        Path videoLocation = event.getVideoLocation();

        if (videoLocation != null) {

            videoLocation = Paths.get(videoLocation.toString().replace("flv", "mp4"));

            final Path rootDir = Paths.get(reporterConfiguration.getRootDirectory());
            final Path relativize = rootDir.relativize(videoLocation);

            final Method testMethod = getTestMethod(event);
            Reporter.createReport(new TestMethodReport(testMethod.getName()))
                    .addKeyValueEntry(DockerEnvironmentReportKey.VIDEO_PATH, new FileEntry(relativize))
                    .inSection(new TestMethodSection(testMethod))
                    .fire(reportEvent);

        }
    }

    private Method getTestMethod(@Observes AfterVideoRecorded event) {
        return event.getAfter().getTestMethod();
    }
}
