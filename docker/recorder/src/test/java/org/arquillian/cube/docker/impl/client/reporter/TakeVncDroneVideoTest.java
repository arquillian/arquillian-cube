package org.arquillian.cube.docker.impl.client.reporter;

import org.arquillian.cube.docker.drone.event.AfterVideoRecorded;
import org.arquillian.recorder.reporter.PropertyEntry;
import org.arquillian.recorder.reporter.ReporterConfiguration;
import org.arquillian.recorder.reporter.event.PropertyReportEvent;
import org.arquillian.recorder.reporter.model.entry.VideoEntry;
import org.jboss.arquillian.core.api.Event;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class TakeVncDroneVideoTest {

    @Mock
    Event<PropertyReportEvent> propertyReportEvent;

    @Captor
    ArgumentCaptor<PropertyReportEvent> propertyReportEventArgumentCaptor;

    @Test
    public void should_create_video_from_root_directory() {

        TakeVncDroneVideo takeVncDroneVideo = new TakeVncDroneVideo();
        takeVncDroneVideo.propertyReportEvent = propertyReportEvent;

        AfterVideoRecorded afterVideoRecorded = new AfterVideoRecorded(Paths.get("target/myvideo.flv"));
        takeVncDroneVideo.reportScreencastRecording(afterVideoRecorded, new ReporterConfiguration());

        verify(propertyReportEvent).fire(propertyReportEventArgumentCaptor.capture());
        final PropertyEntry propertyEntry = propertyReportEventArgumentCaptor.getValue().getPropertyEntry();

        assertThat(propertyEntry).isInstanceOf(VideoEntry.class);
        VideoEntry videoEntry = (VideoEntry) propertyEntry;
        assertThat(videoEntry.getLink()).isEqualTo("myvideo.mp4");

    }

    @Test
    public void should_create_video_from_surefire_report_directory() {

        TakeVncDroneVideo takeVncDroneVideo = new TakeVncDroneVideo();
        takeVncDroneVideo.propertyReportEvent = propertyReportEvent;

        AfterVideoRecorded afterVideoRecorded = new AfterVideoRecorded(Paths.get("target/surefire-report/myvideo.flv"));
        takeVncDroneVideo.reportScreencastRecording(afterVideoRecorded, new ReporterConfiguration());

        verify(propertyReportEvent).fire(propertyReportEventArgumentCaptor.capture());
        final PropertyEntry propertyEntry = propertyReportEventArgumentCaptor.getValue().getPropertyEntry();

        assertThat(propertyEntry).isInstanceOf(VideoEntry.class);
        VideoEntry videoEntry = (VideoEntry) propertyEntry;
        assertThat(videoEntry.getLink()).isEqualTo("surefire-report/myvideo.mp4");

    }

}
