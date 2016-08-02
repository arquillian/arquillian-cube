package org.arquillian.cube.docker.impl.client.reporter;

import com.github.dockerjava.api.model.Version;
import org.arquillian.cube.docker.impl.client.CubeDockerConfiguration;
import org.arquillian.cube.docker.impl.docker.DockerClientExecutor;
import org.arquillian.cube.spi.event.lifecycle.AfterAutoStart;
import org.arquillian.recorder.reporter.PropertyEntry;
import org.arquillian.recorder.reporter.ReporterConfiguration;
import org.arquillian.recorder.reporter.event.PropertyReportEvent;
import org.arquillian.recorder.reporter.model.entry.GroupEntry;
import org.arquillian.recorder.reporter.model.entry.KeyValueEntry;
import org.arquillian.recorder.reporter.model.entry.ScreenshotEntry;
import org.jboss.arquillian.core.api.Event;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TakeDockerEnvironmentTest {

    private static final String MULTIPLE_PORT_BINDING_SCENARIO =
            "helloworld:\n" +
            "  image: dockercloud/hello-world\n" +
            "  exposedPorts: [8089/tcp]\n" +
            "  networkMode: host\n" +
            "  portBindings: [8080->80/tcp, 8081->81/tcp]";

    @Mock
    DockerClientExecutor dockerClientExecutor;

    @Mock
    Version version;

    @Mock
    Event<PropertyReportEvent> propertyReportEvent;

    @Captor
    ArgumentCaptor<PropertyReportEvent> propertyReportEventArgumentCaptor;

    @Before
    public void configureDockerExecutor() {
        when(version.getVersion()).thenReturn("1.1.0");
        when(version.getOperatingSystem()).thenReturn("linux");
        when(version.getKernelVersion()).thenReturn("3.1.0");
        when(version.getApiVersion()).thenReturn("1.12");
        when(version.getArch()).thenReturn("x86");
        when(dockerClientExecutor.dockerHostVersion()).thenReturn(version);
    }

    @Test
    public void should_report_docker_version() {
        final TakeDockerEnvironment takeDockerEnvironment = new TakeDockerEnvironment();
        takeDockerEnvironment.propertyReportEvent = propertyReportEvent;

        Map<String, String> configuration = new HashMap<>();
        configuration.put(CubeDockerConfiguration.DOCKER_CONTAINERS, MULTIPLE_PORT_BINDING_SCENARIO);

        takeDockerEnvironment.reportDockerEnvironment(new AfterAutoStart(), CubeDockerConfiguration.fromMap(configuration, null), dockerClientExecutor, new ReporterConfiguration());

        verify(propertyReportEvent).fire(propertyReportEventArgumentCaptor.capture());

        final PropertyReportEvent propertyReportEvent = propertyReportEventArgumentCaptor.getValue();
        final PropertyEntry propertyEntry = propertyReportEvent.getPropertyEntry();
        assertThat(propertyEntry).isInstanceOf(GroupEntry.class);

        GroupEntry parent = (GroupEntry) propertyEntry;
        final List<PropertyEntry> rootEntries = parent.getPropertyEntries();
        assertThat(rootEntries).hasSize(2);

        final PropertyEntry propertyDockerInfoEntry = rootEntries.get(0);
        assertThat(propertyDockerInfoEntry).isInstanceOf(GroupEntry.class);

        GroupEntry dockerInfoEntry = (GroupEntry) propertyDockerInfoEntry;
        final List<PropertyEntry> propertyEntries = dockerInfoEntry.getPropertyEntries();

        assertThat(propertyEntries).hasSize(5);

        assertThat(propertyEntries.get(0)).isEqualToComparingFieldByField(new KeyValueEntry("Version", "1.1.0"));
        assertThat(propertyEntries.get(1)).isEqualToComparingFieldByField(new KeyValueEntry("OS", "linux"));
        assertThat(propertyEntries.get(2)).isEqualToComparingFieldByField(new KeyValueEntry("Kernel", "3.1.0"));
        assertThat(propertyEntries.get(3)).isEqualToComparingFieldByField(new KeyValueEntry("ApiVersion", "1.12"));
        assertThat(propertyEntries.get(4)).isEqualToComparingFieldByField(new KeyValueEntry("Arch", "x86"));

    }

    @Test
    public void should_report_schema_of_docker_composition() {
        final TakeDockerEnvironment takeDockerEnvironment = new TakeDockerEnvironment();
        takeDockerEnvironment.propertyReportEvent = propertyReportEvent;

        Map<String, String> configuration = new HashMap<>();
        configuration.put(CubeDockerConfiguration.DOCKER_CONTAINERS, MULTIPLE_PORT_BINDING_SCENARIO);

        takeDockerEnvironment.reportDockerEnvironment(new AfterAutoStart(), CubeDockerConfiguration.fromMap(configuration, null), dockerClientExecutor, new ReporterConfiguration());

        verify(propertyReportEvent).fire(propertyReportEventArgumentCaptor.capture());

        final PropertyReportEvent propertyReportEvent = propertyReportEventArgumentCaptor.getValue();
        final PropertyEntry propertyEntry = propertyReportEvent.getPropertyEntry();
        assertThat(propertyEntry).isInstanceOf(GroupEntry.class);

        GroupEntry parent = (GroupEntry) propertyEntry;
        final List<PropertyEntry> rootEntries = parent.getPropertyEntries();
        assertThat(rootEntries).hasSize(2);

        final PropertyEntry compositionsEntry = rootEntries.get(1);
        assertThat(compositionsEntry).isInstanceOf(GroupEntry.class);

        GroupEntry compositionsGroupEntry = (GroupEntry) compositionsEntry;
        final List<PropertyEntry> propertyEntries = compositionsGroupEntry.getPropertyEntries();

        assertThat(propertyEntries).hasSize(1);

        PropertyEntry propertyScreenshotEntry = propertyEntries.get(0);

        assertThat(propertyScreenshotEntry).isInstanceOf(ScreenshotEntry.class);

        ScreenshotEntry screenshotEntry = (ScreenshotEntry) propertyScreenshotEntry;
        assertThat(screenshotEntry.getLink()).isEqualTo("target/schemas/docker_composition.png");
    }

}
