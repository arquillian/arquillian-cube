package org.arquillian.cube.docker.impl.client.reporter;

import com.github.dockerjava.api.model.Statistics;
import com.github.dockerjava.api.model.Version;
import com.mxgraph.layout.hierarchical.mxHierarchicalLayout;
import com.mxgraph.layout.mxIGraphLayout;
import com.mxgraph.util.mxCellRenderer;
import com.mxgraph.view.mxGraph;
import org.arquillian.cube.docker.impl.client.CubeDockerConfiguration;
import org.arquillian.cube.docker.impl.client.config.CubeContainer;
import org.arquillian.cube.docker.impl.client.config.DockerCompositions;
import org.arquillian.cube.docker.impl.client.config.Link;
import org.arquillian.cube.docker.impl.docker.DockerClientExecutor;
import org.arquillian.cube.docker.impl.util.NumberType;
import org.arquillian.cube.spi.Cube;
import org.arquillian.cube.spi.CubeRegistry;
import org.arquillian.cube.spi.event.lifecycle.AfterAutoStart;
import org.arquillian.extension.recorder.When;
import org.arquillian.recorder.reporter.PropertyEntry;
import org.arquillian.recorder.reporter.ReporterConfiguration;
import org.arquillian.recorder.reporter.event.PropertyReportEvent;
import org.arquillian.recorder.reporter.model.entry.FileEntry;
import org.arquillian.recorder.reporter.model.entry.GroupEntry;
import org.arquillian.recorder.reporter.model.entry.KeyValueEntry;
import org.arquillian.recorder.reporter.model.entry.ScreenshotEntry;
import org.jboss.arquillian.core.api.Event;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.test.spi.event.suite.After;
import org.jboss.arquillian.test.spi.event.suite.Before;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;


import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class that reports generic Docker information like orchestration or docker version.
 */
public class TakeDockerEnvironment {

    private static Logger log = Logger.getLogger(TakeDockerEnvironment.class.getName());
    private static ScreenshotEntry EMPTY_SCREENSHOT = new ScreenshotEntry();

    @Inject
    Event<org.arquillian.recorder.reporter.event.PropertyReportEvent> propertyReportEvent;


    public void reportDockerEnvironment(@Observes AfterAutoStart event, CubeDockerConfiguration cubeDockerConfiguration, DockerClientExecutor executor, ReporterConfiguration reporterConfiguration) {

        GroupEntry docker = new GroupEntry("Cube Environment");

        GroupEntry dockerInfo = createDockerInfoGroup(executor);
        docker.getPropertyEntries().add(dockerInfo);

        GroupEntry containersComposition = createDockerCompositionSchema(cubeDockerConfiguration, reporterConfiguration);
        docker.getPropertyEntries().add(containersComposition);

        propertyReportEvent.fire(new PropertyReportEvent(docker));
    }

    public void reportContainerStatsBeforeTest(@Observes Before before, DockerClientExecutor executor, CubeRegistry cubeRegistry) {
        generateStats(executor, cubeRegistry, false, "Before");
    }

    public void reportContainerStatsAfterTest(@Observes After after, DockerClientExecutor executor, CubeRegistry cubeRegistry) {
        generateStats(executor, cubeRegistry, false, "After");
    }

    private void generateStats(DockerClientExecutor executor, CubeRegistry cubeRegistry, Boolean decimal, String when) {
        if (executor != null) {
            List<Cube<?>> containers = cubeRegistry.getCubes();

            GroupEntry containersStat = new GroupEntry("Container Statistics " + when + " Method");

            for (Cube<?> container : containers) {
                String name = container.getId();
                Statistics statistics = null;
                try {
                    statistics = executor.statsContainer(name);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Map<String, Map<String, ?>> stats = getStats(statistics, decimal);
                containersStat.getPropertyEntries().add(createContainerStatGroup(stats, name));
            }
            propertyReportEvent.fire(new PropertyReportEvent(containersStat));
        }
    }

    private GroupEntry createContainerStatGroup(Map<String, Map<String, ?>> stats, String id) {

        GroupEntry containerStatsInfo = new GroupEntry(id + " Statistics");

        for (Map.Entry<String, Map<String, ?>> stat : stats.entrySet()) {
            GroupEntry statType = new GroupEntry(stat.getKey() + " statistics");

            Map<String, ?> nwStats = stat.getValue();
            for (Map.Entry<String, ?> entry : nwStats.entrySet()) {
                GroupEntry info = new GroupEntry(entry.getKey() + " statistics");

                if (entry.getValue() instanceof LinkedHashMap) {

                    Map<String, String> nwstats = (LinkedHashMap) entry.getValue();

                    for (Map.Entry<String, String> nwstat : nwstats.entrySet()) {
                        addEntry(new KeyValueEntry(nwstat.getKey(), nwstat.getValue()), info);
                    }

                } else {

                    addEntry(new KeyValueEntry(entry.getKey(), (String) entry.getValue()), info);
                }
                statType.getPropertyEntries().add(info);
            }
            containerStatsInfo.getPropertyEntries().add(statType);
        }
        return containerStatsInfo;
    }

    public void reportContainerLogs(@Observes org.arquillian.cube.spi.event.lifecycle.BeforeStop beforeStop, DockerClientExecutor executor, ReporterConfiguration reporterConfiguration) {
        final String cubeId = beforeStop.getCubeId();
        if (cubeId != null) {
            final File logFile = new File(createContainerLogDirectory(reporterConfiguration.getRootDir()), cubeId + ".log");
            try {
                executor.copyLog(beforeStop.getCubeId(), false, true, true, true, -1, new FileOutputStream(logFile));
            } catch (IOException e) {
                e.printStackTrace();
            }
            FileEntry fileEntry = new FileEntry();
            fileEntry.setPath(logFile.getPath());
            fileEntry.setType("Log");
            fileEntry.setMessage("Logs of " + cubeId + " container before stop event.");
            propertyReportEvent.fire(new PropertyReportEvent(fileEntry));
        }
    }


    private GroupEntry createDockerCompositionSchema(CubeDockerConfiguration cubeDockerConfiguration, ReporterConfiguration reporterConfiguration) {

        final GroupEntry containersComposition = new GroupEntry("Containers Composition");

        final mxGraph graph = new mxGraph();
        final Object parent = graph.getDefaultParent();

        graph.setAutoSizeCells(true);
        graph.getModel().beginUpdate();

        try {

            final DockerCompositions dockerContainersContent = cubeDockerConfiguration.getDockerContainersContent();
            final Map<String, CubeContainer> containers = dockerContainersContent.getContainers();

            final Map<String, Object> insertedVertex = new HashMap<>();

            for (Map.Entry<String, CubeContainer> containerEntry : containers.entrySet()) {

                String containerId = containerEntry.getKey();
                CubeContainer cubeContainer = containerEntry.getValue();

                updateGraph(graph, parent, insertedVertex, containerId, cubeContainer);

            }

        } finally {
            graph.getModel().endUpdate();
        }

        mxIGraphLayout layout = new mxHierarchicalLayout(graph, SwingConstants.WEST);
        layout.execute(graph.getDefaultParent());

        ScreenshotEntry screenshotEntry = generateCompositionSchemaImage(graph, reporterConfiguration);
        addEntry(screenshotEntry, containersComposition);

        return containersComposition;
    }

    private void updateGraph(mxGraph graph, Object parent, Map<String, Object> insertedVertex, String containerId, CubeContainer cubeContainer) {
        if (insertedVertex.containsKey(containerId)) {
            // container is already added, probably because a direct link from another container
            // now we need to add direct links of this one that before were transitive
            Object currentContainer = insertedVertex.get(containerId);
            createDirectLinks(graph, parent, insertedVertex, cubeContainer, currentContainer);

        } else {
            // create new cube and possible direct link (not transitive ones)
            Object currentContainer = graph.insertVertex(parent, null, containerId, 0, 0, 80, 30);
            graph.updateCellSize(currentContainer);
            insertedVertex.put(containerId, currentContainer);

            createDirectLinks(graph, parent, insertedVertex, cubeContainer, currentContainer);
        }
    }

    private void createDirectLinks(mxGraph graph, Object parent, Map<String, Object> insertedVertex, CubeContainer cubeContainer, Object currentContainer) {
        // create relation to all direct links
        if (cubeContainer.getLinks() != null) {
            for (Link link : cubeContainer.getLinks()) {
                final String linkId = link.getName();

                Object linkContainer = null;
                if (insertedVertex.containsKey(linkId)) {
                    linkContainer = insertedVertex.get(linkId);
                } else {
                    linkContainer = graph.insertVertex(parent, null, linkId, 0, 0, 80, 30);
                }

                graph.updateCellSize(currentContainer);
                graph.insertEdge(parent, null, link.getAlias(), currentContainer, linkContainer);
                insertedVertex.put(linkId, linkContainer);
            }
        }
    }

    private ScreenshotEntry generateCompositionSchemaImage(mxGraph graph, ReporterConfiguration reporterConfiguration) {
        final BufferedImage bufferedImage = mxCellRenderer.createBufferedImage(graph, null, 1, Color.WHITE, true, null);

        final File imageFile = new File(createSchemasDirectory(reporterConfiguration.getRootDir()), "docker_composition.png");
        try {
            ImageIO.write(bufferedImage, "PNG", imageFile);

            final Path rootDir = Paths.get(reporterConfiguration.getRootDir().getName());
            final Path relativize = rootDir.relativize(imageFile.toPath());

            ScreenshotEntry screenshotEntry = new ScreenshotEntry();
            screenshotEntry.setPhase(When.BEFORE);
            screenshotEntry.setPath(relativize.toString());
            screenshotEntry.setLink(relativize.toString());
            screenshotEntry.setWidth(bufferedImage.getWidth());
            screenshotEntry.setHeight(bufferedImage.getHeight());
            screenshotEntry.setSize(String.valueOf(imageFile.length()));
            screenshotEntry.setMessage("Docker Composition before executing tests.");

            return screenshotEntry;
        } catch (IOException e) {
            log.log(Level.WARNING, String.format("Docker compositions schema could not be generated because of %s.", e));
        }

        return EMPTY_SCREENSHOT;
    }

    private GroupEntry createDockerInfoGroup(DockerClientExecutor executor) {
        GroupEntry dockerInfo = new GroupEntry("Docker Info");

        Version version = executor.dockerHostVersion();
        addEntry(new KeyValueEntry("Version", version.getVersion()), dockerInfo);
        addEntry(new KeyValueEntry("OS", version.getOperatingSystem()), dockerInfo);
        addEntry(new KeyValueEntry("Kernel", version.getKernelVersion()), dockerInfo);
        addEntry(new KeyValueEntry("ApiVersion", version.getApiVersion()), dockerInfo);
        addEntry(new KeyValueEntry("Arch", version.getArch()), dockerInfo);
        return dockerInfo;
    }

    private File createSchemasDirectory(File rootDirectory) {
        final Path reportsSchema = Paths.get("reports", "schemas");
        final Path schemasDir = rootDirectory.toPath().resolve(reportsSchema);

        try {
            Files.createDirectories(schemasDir);
        } catch (IOException e) {
            throw new IllegalArgumentException(String.format("Could not created schemas directory at %s", schemasDir));
        }

        return schemasDir.toFile();
    }

    private File createContainerLogDirectory(File rootDirectory) {
        final Path reportsLogs = Paths.get("reports", "logs");
        final Path logsDir = rootDirectory.toPath().resolve(reportsLogs);
        if (Files.notExists(logsDir)) {
            try {

                Files.createDirectories(logsDir);
            } catch (IOException e) {
                throw new IllegalArgumentException(String.format("Could not created logs directory at %s", logsDir));
            }
        }
        return logsDir.toFile();
    }

    private void addEntry(PropertyEntry propertyEntry, GroupEntry groupEntry) {
        groupEntry.getPropertyEntries().add(propertyEntry);
    }

    private Map<String, Map<String, ?>> getStats(Statistics statistics, Boolean decimal) {

        Map<String, Map<String, ?>> stats = new LinkedHashMap<>();
        if (statistics != null){
            Map<String, Map<String, String>> networks = extractNetworksStats(statistics.getNetworks(), decimal);
            Map<String, String> memory = extractMemoryStats(statistics.getMemoryStats(), decimal, "usage", "max_usage", "limit");
            Map<String, String> blkio = extractIORW(statistics.getBlkioStats(), decimal);
            stats.put("network", networks);
            stats.put("memory", memory);
            stats.put("block I/O", blkio);
        }
        return stats;
    }

    private Map<String, Map<String, String>> extractNetworksStats(Map<String, Object> map, boolean decimal) {
        Map<String, Map<String, String>> nwStatsForEachNICAndTotal = new LinkedHashMap<>();
        if (map != null) {
            long rx_bytes = 0, tx_bytes = 0;

            for (Map.Entry<String, Object> entry: map.entrySet()) {
                Map<String, String> nwStats = new LinkedHashMap<>();
                String adapterName = entry.getKey();
                if (entry.getValue() instanceof LinkedHashMap) {

                    Map<String, ?> adapter = (LinkedHashMap) entry.getValue();

                    long rx_bytes_ = convertToLong(adapter.get("rx_bytes"));
                    long tx_bytes_ = convertToLong(adapter.get("tx_bytes"));

                    nwStats.put("rx_bytes", humanReadableByteCount(rx_bytes_, decimal));
                    nwStats.put("tx_bytes", humanReadableByteCount(tx_bytes_, decimal));
                    nwStatsForEachNICAndTotal.put(adapterName, nwStats);

                    rx_bytes += rx_bytes_;
                    tx_bytes += tx_bytes_;
                }
            }

            Map<String, String> total = new LinkedHashMap<>();

            total.put("rx_bytes", humanReadableByteCount(rx_bytes, decimal));
            total.put("tx_bytes", humanReadableByteCount(tx_bytes, decimal));
            nwStatsForEachNICAndTotal.put("Total", total);
        }
        return nwStatsForEachNICAndTotal;
    }

    private Map<String, String> extractIORW(Map<String, Object> blkioStats, Boolean decimal) {
        Map<String, String> blkrwStats = new LinkedHashMap<>();
        if (blkioStats != null) {
            List<LinkedHashMap> bios = (ArrayList<LinkedHashMap>) blkioStats.get("io_service_bytes_recursive");
            long read = 0, write = 0;
            for (Map<String, ?> io: bios) {
                if (io != null) {
                    switch ((String) io.get("op")) {
                        case "Read":
                            read = convertToLong(io.get("value"));
                            break;
                        case "Write":
                            write = convertToLong(io.get("value"));
                            break;
                    }
                }
            }
            blkrwStats.put("I/O Bytes Read", humanReadableByteCount(read, decimal));
            blkrwStats.put("I/O Bytes Write", humanReadableByteCount(write, decimal));
        }
        return blkrwStats;
    }

    private Map<String, String> extractMemoryStats(Map<String, Object> map, boolean decimal, String... fields) {
        Map<String, String> memory = new LinkedHashMap<>();
        if (map != null) {
            for (String field: fields) {
                long usage = convertToLong(map.get(field));
                memory.put(field, humanReadableByteCount(usage, decimal));
            }
        }
        return memory;
    }

    private String humanReadableByteCount(Long bytes, boolean decimal) {
        int unit = decimal ? 1000 : 1024;
        if (bytes < unit) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = (decimal ? "kMGTPE" : "KMGTPE").charAt(exp-1) + (decimal ? "" : "i");

        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }

    private long convertToLong(Object number) {
        long longNumber = 0;

        NumberType type = NumberType.valueOf(number.getClass().getSimpleName().toUpperCase());

        switch (type) {
            case BYTE:
                longNumber = ((Byte) number).longValue();
                break;
            case SHORT:
                longNumber = ((Short) number).longValue();
                break;
            case INTEGER:
                longNumber = ((Integer) number).longValue();
                break;
            case LONG:
                longNumber = (long) number;
                break;
        }
        return longNumber;
    }
}
