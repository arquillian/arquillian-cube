package org.arquillian.cube.docker.impl.client.reporter;

import com.github.dockerjava.api.model.Statistics;
import com.github.dockerjava.api.model.Version;
import org.arquillian.cube.docker.impl.client.CubeDockerConfiguration;
import org.arquillian.cube.docker.impl.docker.DockerClientExecutor;
import org.arquillian.cube.impl.model.LocalCubeRegistry;
import org.arquillian.cube.spi.Cube;
import org.arquillian.cube.spi.CubeRegistry;
import org.arquillian.cube.spi.event.lifecycle.AfterAutoStart;
import org.arquillian.cube.spi.event.lifecycle.BeforeStop;
import org.arquillian.recorder.reporter.PropertyEntry;
import org.arquillian.recorder.reporter.ReporterConfiguration;
import org.arquillian.recorder.reporter.event.PropertyReportEvent;
import org.arquillian.recorder.reporter.model.entry.FileEntry;
import org.arquillian.recorder.reporter.model.entry.GroupEntry;
import org.arquillian.recorder.reporter.model.entry.KeyValueEntry;
import org.arquillian.recorder.reporter.model.entry.ScreenshotEntry;
import org.jboss.arquillian.core.api.Event;
import org.jboss.arquillian.test.spi.event.suite.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
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

    private static final String CUBE_ID = "tomcat";

    @Mock
    DockerClientExecutor dockerClientExecutor;

    @Mock
    Version version;

    @Mock
    Event<PropertyReportEvent> propertyReportEvent;

    @Mock
    private Cube cube;

    @Captor
    ArgumentCaptor<PropertyReportEvent> propertyReportEventArgumentCaptor;

    private CubeRegistry cubeRegistry;

    private Statistics statistics;

    @Before
    public void configureDockerExecutorAndCubeRegistry() throws IOException {
        configureDockerExecutor();
        configureCube();
    }

    private void configureDockerExecutor() {
        when(version.getVersion()).thenReturn("1.1.0");
        when(version.getOperatingSystem()).thenReturn("linux");
        when(version.getKernelVersion()).thenReturn("3.1.0");
        when(version.getApiVersion()).thenReturn("1.12");
        when(version.getArch()).thenReturn("x86");
        when(dockerClientExecutor.dockerHostVersion()).thenReturn(version);
    }

    private void configureCube() throws IOException {
        cubeRegistry = new LocalCubeRegistry();
        statistics = new Statistics();
        cubeRegistry.addCube(cube);
        when(cube.getId()).thenReturn(CUBE_ID);
        when(dockerClientExecutor.statsContainer(CUBE_ID)).thenReturn(statistics);
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
        assertThat(screenshotEntry.getLink()).isEqualTo("reports/schemas/docker_composition.png");
    }

   @Test
    public void should_report_log_file() throws IOException {
        final TakeDockerEnvironment takeDockerEnvironment = new TakeDockerEnvironment();
        takeDockerEnvironment.propertyReportEvent = propertyReportEvent;
        takeDockerEnvironment.reportContainerLogs(new BeforeStop(CUBE_ID), dockerClientExecutor, new ReporterConfiguration());
        verify(propertyReportEvent).fire(propertyReportEventArgumentCaptor.capture());

        final PropertyReportEvent propertyReportEvent = propertyReportEventArgumentCaptor.getValue();
        final PropertyEntry propertyEntry = propertyReportEvent.getPropertyEntry();
        assertThat(propertyEntry).isInstanceOf(FileEntry.class);

        FileEntry fileEntry = (FileEntry) propertyEntry;
        assertThat(fileEntry.getPath()).isEqualTo("target/reports/logs/tomcat.log");
    }

    @Test
    public void should_report_container_stats() throws IOException, NoSuchMethodException {
        final TakeDockerEnvironment takeDockerEnvironment = new TakeDockerEnvironment();
        takeDockerEnvironment.propertyReportEvent = propertyReportEvent;

        takeDockerEnvironment.reportContainerStatsAfterTest(new After(TakeDockerEnvironmentTest.class, TakeDockerEnvironmentTest.class.getMethod("shouldReportStats")), dockerClientExecutor, cubeRegistry);
        verify(propertyReportEvent).fire(propertyReportEventArgumentCaptor.capture());

        final PropertyReportEvent propertyReportEvent = propertyReportEventArgumentCaptor.getValue();
        final PropertyEntry propertyEntry = propertyReportEvent.getPropertyEntry();
        assertThat(propertyEntry).isInstanceOf(GroupEntry.class);

        GroupEntry parent = (GroupEntry) propertyEntry;
        final List<PropertyEntry> rootEntries = parent.getPropertyEntries();
        assertThat(rootEntries).hasSize(1);

        List<PropertyEntry> entryList = rootEntries.get(0).getPropertyEntries();

        assertThat(entryList).hasSize(3).extracting("class.simpleName").containsExactly("GroupEntry", "GroupEntry", "GroupEntry");
        assertThat(entryList).extracting("name").contains("network statistics", "memory statistics", "block I/O statistics");
    }

}
