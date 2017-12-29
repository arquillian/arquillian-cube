package org.arquillian.cube.docker.impl.client.reporter;

import org.arquillian.cube.docker.drone.event.AfterVideoRecorded;
import org.arquillian.cube.docker.impl.requirement.RequiresDocker;
import org.arquillian.cube.docker.impl.requirement.RequiresDockerMachine;
import org.arquillian.reporter.api.builder.BuilderLoader;
import org.arquillian.reporter.api.event.SectionEvent;
import org.arquillian.reporter.api.model.entry.FileEntry;
import org.arquillian.reporter.api.model.entry.KeyValueEntry;
import org.arquillian.reporter.api.model.report.Report;
import org.arquillian.reporter.api.model.report.TestMethodReport;
import org.arquillian.reporter.config.ReporterConfiguration;
import org.jboss.arquillian.core.api.Event;
import org.jboss.arquillian.test.spi.event.suite.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.lang.reflect.Method;
import java.nio.file.Paths;
import java.util.LinkedHashMap;

import static org.arquillian.reporter.impl.asserts.ReportAssert.assertThatReport;
import static org.arquillian.reporter.impl.asserts.SectionAssert.assertThatSection;
import static org.mockito.Mockito.verify;

@Category({RequiresDocker.class, RequiresDockerMachine.class})
@RunWith(MockitoJUnitRunner.class)
public class TakeVncDroneVideoTest {

    @Before
    public void setUp() {
        BuilderLoader.load();
    }

    @Mock
    Event<SectionEvent> reportEvent;

    @Captor
    ArgumentCaptor<SectionEvent> reportEventArgumentCaptor;

    @Test
    public void should_create_video_from_root_directory() throws NoSuchMethodException {
        //given
        TakeVncDroneVideo takeVncDroneVideo = new TakeVncDroneVideo();
        takeVncDroneVideo.reportEvent = reportEvent;

        final Method method = getMethod("should_create_video_from_root_directory");
        AfterVideoRecorded afterVideoRecorded = new AfterVideoRecorded(new After(TakeDockerEnvironmentTest.class, method), Paths.get("target/myvideo.flv"));

        //when
        takeVncDroneVideo.reportScreencastRecording(afterVideoRecorded, getReporterConfiguration());

        //then
        verify(reportEvent).fire(reportEventArgumentCaptor.capture());
        final SectionEvent sectionEvent = reportEventArgumentCaptor.getValue();

        final String methodName = method.getName();

        assertThatSection(sectionEvent)
                .hasSectionId(methodName)
                .hasReportOfTypeThatIsAssignableFrom(TestMethodReport.class);

        final Report report = sectionEvent.getReport();

        assertThatReport(report)
                .hasName(methodName)
                .hasNumberOfEntries(1)
                .hasEntriesContaining(new KeyValueEntry(DockerEnvironmentReportKey.VIDEO_PATH, new FileEntry("myvideo.mp4")));
    }

    @Test
    public void should_create_video_from_surefire_report_directory() throws NoSuchMethodException {
        //given
        TakeVncDroneVideo takeVncDroneVideo = new TakeVncDroneVideo();
        takeVncDroneVideo.reportEvent = reportEvent;

        final Method method = getMethod("should_create_video_from_surefire_report_directory");
        AfterVideoRecorded afterVideoRecorded = new AfterVideoRecorded(new After(TakeDockerEnvironmentTest.class, method), Paths.get("target/surefire-report/myvideo.flv"));

        //when
        takeVncDroneVideo.reportScreencastRecording(afterVideoRecorded, getReporterConfiguration());

        //then
        verify(reportEvent).fire(reportEventArgumentCaptor.capture());
        final SectionEvent sectionEvent = reportEventArgumentCaptor.getValue();

        final String methodName = method.getName();

        assertThatSection(sectionEvent)
                .hasSectionId(methodName)
                .hasReportOfTypeThatIsAssignableFrom(TestMethodReport.class);

        final Report report = sectionEvent.getReport();

        assertThatReport(report)
                .hasName(methodName)
                .hasNumberOfEntries(1)
                .hasEntriesContaining(new KeyValueEntry(DockerEnvironmentReportKey.VIDEO_PATH, new FileEntry("surefire-report/myvideo.mp4")));
    }

    private ReporterConfiguration getReporterConfiguration() {
        return ReporterConfiguration.fromMap(new LinkedHashMap<>());
    }

    private Method getMethod(String name) throws NoSuchMethodException {
        return TakeVncDroneVideoTest.class.getMethod(name);
    }

}
