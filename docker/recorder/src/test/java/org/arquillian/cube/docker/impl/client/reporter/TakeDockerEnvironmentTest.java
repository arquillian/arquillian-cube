package org.arquillian.cube.docker.impl.client.reporter;

import com.github.dockerjava.api.model.Statistics;
import com.github.dockerjava.api.model.Version;
import org.arquillian.cube.docker.impl.client.CubeDockerConfiguration;
import org.arquillian.cube.docker.impl.client.DefinitionFormat;
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
import org.arquillian.recorder.reporter.model.entry.table.TableCellEntry;
import org.arquillian.recorder.reporter.model.entry.table.TableEntry;
import org.arquillian.recorder.reporter.model.entry.table.TableRowEntry;
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
import java.util.LinkedHashMap;
import java.util.ArrayList;

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

    @Mock
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
        when(cube.getId()).thenReturn(CUBE_ID);
        cubeRegistry.addCube(cube);
        when(statistics.getNetworks()).thenReturn(getNetworks());
        when(statistics.getMemoryStats()).thenReturn(getMemory());
        when(statistics.getBlkioStats()).thenReturn(getIOStats());
        when(dockerClientExecutor.statsContainer(CUBE_ID)).thenReturn(statistics);
    }

    @Test
    public void should_report_docker_version() {
        final TakeDockerEnvironment takeDockerEnvironment = new TakeDockerEnvironment();
        takeDockerEnvironment.propertyReportEvent = propertyReportEvent;

        Map<String, String> configuration = new HashMap<>();
        configuration.put(CubeDockerConfiguration.DOCKER_CONTAINERS, MULTIPLE_PORT_BINDING_SCENARIO);
        configuration.put("definitionFormat", DefinitionFormat.CUBE.name());

        takeDockerEnvironment.reportDockerEnvironment(new AfterAutoStart(), CubeDockerConfiguration.fromMap(configuration, null), dockerClientExecutor, new ReporterConfiguration());

        verify(propertyReportEvent).fire(propertyReportEventArgumentCaptor.capture());

        final PropertyReportEvent propertyReportEvent = propertyReportEventArgumentCaptor.getValue();
        final PropertyEntry propertyEntry = propertyReportEvent.getPropertyEntry();
        assertThat(propertyEntry).isInstanceOf(GroupEntry.class);

        GroupEntry parent = (GroupEntry) propertyEntry;
        final List<PropertyEntry> rootEntries = parent.getPropertyEntries();
        assertThat(rootEntries).hasSize(3);

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
        configuration.put("definitionFormat", DefinitionFormat.CUBE.name());

        takeDockerEnvironment.reportDockerEnvironment(new AfterAutoStart(), CubeDockerConfiguration.fromMap(configuration, null), dockerClientExecutor, new ReporterConfiguration());

        verify(propertyReportEvent).fire(propertyReportEventArgumentCaptor.capture());

        final PropertyReportEvent propertyReportEvent = propertyReportEventArgumentCaptor.getValue();
        final PropertyEntry propertyEntry = propertyReportEvent.getPropertyEntry();
        assertThat(propertyEntry).isInstanceOf(GroupEntry.class);

        GroupEntry parent = (GroupEntry) propertyEntry;
        final List<PropertyEntry> rootEntries = parent.getPropertyEntries();
        assertThat(rootEntries).hasSize(3);

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
        assertThat(propertyEntry).isInstanceOf(GroupEntry.class);

        List<PropertyEntry> childEntry = propertyEntry.getPropertyEntries();
        assertThat(childEntry).hasSize(1).extracting("class.simpleName").containsExactly("FileEntry");

        FileEntry fileEntry = (FileEntry) childEntry.get(0);
        assertThat(fileEntry.getPath()).isEqualTo("reports/logs/tomcat.log");
    }

    @Test
    public void should_report_container_network_stats() throws IOException, NoSuchMethodException {
        final TakeDockerEnvironment takeDockerEnvironment = new TakeDockerEnvironment();
        takeDockerEnvironment.propertyReportEvent = propertyReportEvent;

        takeDockerEnvironment.captureContainerStatsBeforeTest(new org.jboss.arquillian.test.spi.event.suite.Before(TakeDockerEnvironmentTest.class, TakeDockerEnvironmentTest.class.getMethod("should_report_container_memory_stats")), dockerClientExecutor, cubeRegistry);
        takeDockerEnvironment.reportContainerStatsAfterTest(new After(TakeDockerEnvironmentTest.class, TakeDockerEnvironmentTest.class.getMethod("should_report_container_network_stats")), dockerClientExecutor, cubeRegistry);
        verify(propertyReportEvent).fire(propertyReportEventArgumentCaptor.capture());

        final PropertyReportEvent propertyReportEvent = propertyReportEventArgumentCaptor.getValue();
        final PropertyEntry propertyEntry = propertyReportEvent.getPropertyEntry();
        assertThat(propertyEntry).isInstanceOf(GroupEntry.class);

        GroupEntry parent = (GroupEntry) propertyEntry;
        final List<PropertyEntry> rootEntries = parent.getPropertyEntries();
        assertThat(rootEntries).hasSize(3);

        List<PropertyEntry> entryList = rootEntries.get(2).getPropertyEntries();

        assertThat(entryList).hasSize(1).extracting("class.simpleName").containsExactly("TableEntry");

        TableEntry tableEntry = (TableEntry) entryList.get(0);

        assertThat(tableEntry).extracting("tableHead").extracting("row").flatExtracting("cells").
                hasSize(3).containsExactly(new TableCellEntry("Adapter"), new TableCellEntry("rx_bytes", 3, 1), new TableCellEntry("tx_bytes", 3, 1));

        assertThat(tableEntry).extracting("tableBody").flatExtracting("rows").hasSize(3);

        assertThat(tableEntry.getTableBody().getRows().get(1).getCells()).hasSize(7).extractingResultOf("getContent").
                containsExactly("eth0", "724 B", "724 B", "0 B", "418 B", "418 B", "0 B");
        assertThat(tableEntry.getTableBody().getRows().get(2).getCells()).hasSize(7).extractingResultOf("getContent").
                containsExactly("Total", "724 B", "724 B", "0 B", "418 B", "418 B", "0 B");

    }

    @Test
    public void should_report_container_memory_stats() throws IOException, NoSuchMethodException {
        final TakeDockerEnvironment takeDockerEnvironment = new TakeDockerEnvironment();
        takeDockerEnvironment.propertyReportEvent = propertyReportEvent;

        takeDockerEnvironment.captureContainerStatsBeforeTest(new org.jboss.arquillian.test.spi.event.suite.Before(TakeDockerEnvironmentTest.class, TakeDockerEnvironmentTest.class.getMethod("should_report_container_memory_stats")), dockerClientExecutor, cubeRegistry);
        takeDockerEnvironment.reportContainerStatsAfterTest(new After(TakeDockerEnvironmentTest.class, TakeDockerEnvironmentTest.class.getMethod("should_report_container_memory_stats")), dockerClientExecutor, cubeRegistry);
        verify(propertyReportEvent).fire(propertyReportEventArgumentCaptor.capture());

        final PropertyReportEvent propertyReportEvent = propertyReportEventArgumentCaptor.getValue();
        final PropertyEntry propertyEntry = propertyReportEvent.getPropertyEntry();
        assertThat(propertyEntry).isInstanceOf(GroupEntry.class);

        GroupEntry parent = (GroupEntry) propertyEntry;
        final List<PropertyEntry> rootEntries = parent.getPropertyEntries();
        assertThat(rootEntries).hasSize(3);
        List<PropertyEntry> entryList = rootEntries.get(0).getPropertyEntries();

        assertThat(entryList).hasSize(1).extracting("class.simpleName").containsExactly("TableEntry");

        TableEntry tableEntry = (TableEntry) entryList.get(0);

        assertThat(tableEntry).extracting("tableHead").extracting("row").flatExtracting("cells").
                hasSize(3).containsExactly(new TableCellEntry("usage", 3, 1), new TableCellEntry("max_usage", 3, 1), new TableCellEntry("limit"));

        assertThat(tableEntry).extracting("tableBody").flatExtracting("rows").hasSize(2);
        TableRowEntry rowEntry = tableEntry.getTableBody().getRows().get(1);
        assertThat(rowEntry.getCells()).hasSize(7).extractingResultOf("getContent").
                containsExactly("33.51 MiB", "33.51 MiB", "0 B", "34.11 MiB", "34.11 MiB", "0 B", "19.04 GiB");

    }

    @Test
    public void should_report_container_blkio_stats() throws IOException, NoSuchMethodException {
        final TakeDockerEnvironment takeDockerEnvironment = new TakeDockerEnvironment();
        takeDockerEnvironment.propertyReportEvent = propertyReportEvent;
        takeDockerEnvironment.captureContainerStatsBeforeTest(new org.jboss.arquillian.test.spi.event.suite.Before(TakeDockerEnvironmentTest.class, TakeDockerEnvironmentTest.class.getMethod("should_report_container_memory_stats")), dockerClientExecutor, cubeRegistry);
        takeDockerEnvironment.reportContainerStatsAfterTest(new After(TakeDockerEnvironmentTest.class, TakeDockerEnvironmentTest.class.getMethod("should_report_container_blkio_stats")), dockerClientExecutor, cubeRegistry);
        verify(propertyReportEvent).fire(propertyReportEventArgumentCaptor.capture());

        final PropertyReportEvent propertyReportEvent = propertyReportEventArgumentCaptor.getValue();
        final PropertyEntry propertyEntry = propertyReportEvent.getPropertyEntry();
        assertThat(propertyEntry).isInstanceOf(GroupEntry.class);

        GroupEntry parent = (GroupEntry) propertyEntry;
        final List<PropertyEntry> rootEntries = parent.getPropertyEntries();

        assertThat(rootEntries).hasSize(3);
        List<PropertyEntry> entryList = rootEntries.get(1).getPropertyEntries();

        assertThat(entryList).hasSize(1).extracting("class.simpleName").containsExactly("TableEntry");

        TableEntry tableEntry = (TableEntry) entryList.get(0);

        assertThat(tableEntry).extracting("tableHead").extracting("row").flatExtracting("cells").
                hasSize(2).containsExactly(new TableCellEntry("io_bytes_read", 3, 1), new TableCellEntry("io_bytes_write", 3, 1));

        assertThat(tableEntry).extracting("tableBody").flatExtracting("rows").hasSize(2);
        TableRowEntry rowEntry = tableEntry.getTableBody().getRows().get(1);
        assertThat(rowEntry.getCells()).hasSize(6).extractingResultOf("getContent").
                containsExactly("49.50 KiB", "49.50 KiB", "0 B", "0 B", "0 B", "0 B");
    }

    @Test
    public void should_report_network_topology_of_docker_containers() {
        final TakeDockerEnvironment takeDockerEnvironment = new TakeDockerEnvironment();
        takeDockerEnvironment.propertyReportEvent = propertyReportEvent;

        Map<String, String> configuration = new HashMap<>();
        configuration.put(CubeDockerConfiguration.DOCKER_CONTAINERS, MULTIPLE_PORT_BINDING_SCENARIO);
        configuration.put("definitionFormat", DefinitionFormat.CUBE.name());

        takeDockerEnvironment.reportDockerEnvironment(new org.arquillian.cube.spi.event.lifecycle.AfterAutoStart(), CubeDockerConfiguration.fromMap(configuration, null), dockerClientExecutor, new ReporterConfiguration());

        verify(propertyReportEvent).fire(propertyReportEventArgumentCaptor.capture());

        final PropertyReportEvent propertyReportEvent = propertyReportEventArgumentCaptor.getValue();
        final PropertyEntry propertyEntry = propertyReportEvent.getPropertyEntry();
        assertThat(propertyEntry).isInstanceOf(GroupEntry.class);

        GroupEntry parent = (GroupEntry) propertyEntry;
        final List<PropertyEntry> rootEntries = parent.getPropertyEntries();
        assertThat(rootEntries).hasSize(3);

        final PropertyEntry networksEntry = rootEntries.get(2);
        assertThat(networksEntry).isInstanceOf(GroupEntry.class);

        GroupEntry networksGroupEntry = (GroupEntry) networksEntry;
        final List<PropertyEntry> propertyEntries = networksGroupEntry.getPropertyEntries();

        assertThat(propertyEntries).hasSize(1);

        PropertyEntry propertyScreenshotEntry = propertyEntries.get(0);

        assertThat(propertyScreenshotEntry).isInstanceOf(ScreenshotEntry.class);

        ScreenshotEntry screenshotEntry = (ScreenshotEntry) propertyScreenshotEntry;
        assertThat(screenshotEntry.getLink()).isEqualTo("reports/networks/docker_network_topology.png");
    }

    private Map<String, Object> getNetworks(){
        Map<String, Object> nw = new LinkedHashMap<>();
        Map<String, Integer> bytes = new LinkedHashMap<>();
        bytes.put("rx_bytes",724);
        bytes.put("tx_bytes", 418);
        bytes.put("rx_packets", 19);
        nw.put("eth0", bytes);

        return nw;
    }

    private Map<String, Object> getMemory(){
        Map<String, Object> memory = new LinkedHashMap<>();
        memory.put("usage", 35135488);
        memory.put("max_usage", 35770368);
        memory.put("limit", 20444532736L);
        memory.put("stats", new LinkedHashMap<>());

        return memory;
    }

    private Map<String,Object> getIOStats() {
        Map<String, Object> blkIO = new LinkedHashMap<>();
        List<LinkedHashMap<String, ?>> io = new ArrayList<>();
        LinkedHashMap ioServiceRead = new LinkedHashMap();

        ioServiceRead.put("major",7);
        ioServiceRead.put("minor",0);
        ioServiceRead.put("op", "Read");
        ioServiceRead.put("value",50688);
        io.add(ioServiceRead);

        LinkedHashMap ioServiceWrite = new LinkedHashMap();
        ioServiceWrite.put("major",7);
        ioServiceWrite.put("minor",0);
        ioServiceWrite.put("op", "Write");
        ioServiceWrite.put("value",0);
        io.add(ioServiceWrite);

        LinkedHashMap ioServiceSync = new LinkedHashMap();
        ioServiceSync.put("major", 7);
        ioServiceSync.put("minor",0);
        ioServiceSync.put("op", "Sync");
        ioServiceSync.put("value", 0);
        io.add(ioServiceSync);

        blkIO.put("io_service_bytes_recursive", io);
        blkIO.put("io_time_recursive",new ArrayList<>());

        return blkIO;
    }
}
