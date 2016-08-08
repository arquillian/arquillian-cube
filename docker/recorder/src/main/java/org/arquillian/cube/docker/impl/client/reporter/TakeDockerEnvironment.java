package org.arquillian.cube.docker.impl.client.reporter;

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
import org.arquillian.cube.spi.event.lifecycle.AfterAutoStart;
import org.arquillian.extension.recorder.When;
import org.arquillian.recorder.reporter.PropertyEntry;
import org.arquillian.recorder.reporter.ReporterConfiguration;
import org.arquillian.recorder.reporter.event.PropertyReportEvent;
import org.arquillian.recorder.reporter.model.entry.GroupEntry;
import org.arquillian.recorder.reporter.model.entry.KeyValueEntry;
import org.arquillian.recorder.reporter.model.entry.ScreenshotEntry;
import org.jboss.arquillian.core.api.Event;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
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

    private void addEntry(PropertyEntry propertyEntry, GroupEntry groupEntry) {
        groupEntry.getPropertyEntries().add(propertyEntry);
    }

}
