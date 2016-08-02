package org.arquillian.cube.docker.impl.client.metadata;

import org.arquillian.cube.docker.impl.client.config.CubeContainer;
import org.arquillian.cube.docker.impl.model.DockerCube;
import org.arquillian.cube.spi.metadata.CanReportMetrics;
import org.arquillian.recorder.reporter.Reportable;
import org.arquillian.recorder.reporter.model.entry.GroupEntry;
import org.arquillian.recorder.reporter.model.entry.KeyValueEntry;
import org.arquillian.recorder.reporter.model.entry.table.TableCellEntry;
import org.arquillian.recorder.reporter.model.entry.table.TableRowEntry;

import java.security.Key;
import java.util.Collection;

/**
 * Reporting metrics capabilities for Docker Cube.
 */
public class ReportMetrics implements CanReportMetrics {

    private DockerCube dockerCube;

    public ReportMetrics(DockerCube dockerCube) {
        this.dockerCube = dockerCube;
    }

    @Override
    public Reportable report() {

        GroupEntry groupEntry = new GroupEntry(dockerCube.getId());

        boolean error;
        switch(dockerCube.state()) {
            case START_FAILED:
            case CREATE_FAILED:
            case STOP_FAILED:
            case DESTORY_FAILED:
                error = true;
            break;

            default: error = false;
        }

        KeyValueEntry errorKeyValue = new KeyValueEntry("Error during lifecycle", Boolean.toString(error));
        groupEntry.getPropertyEntries().add(errorKeyValue);

        KeyValueEntry startingTime = new KeyValueEntry("Starting Time", String.format("%s ms", dockerCube.getStartingTimeInMillis()));
        KeyValueEntry stoppingTime = new KeyValueEntry("Stopping Time", String.format("%s ms", dockerCube.getStoppingTimeInMillis()));

        groupEntry.getPropertyEntries().add(startingTime);
        groupEntry.getPropertyEntries().add(stoppingTime);

        groupEntry.getPropertyEntries().add(writeProperties(dockerCube.getId(), dockerCube.configuration()));
        return groupEntry;
    }


    private GroupEntry writeProperties(String containerId, CubeContainer cubeContainer) {
        final GroupEntry row = new GroupEntry("Docker Cube Properties");
        row.getPropertyEntries().add(cell("Container Name", containerId));

        KeyValueEntry image = cubeContainer.getImage() != null ? cell("Image", cubeContainer.getImage().toString()) : cell("Build", cubeContainer.getBuildImage().getDockerfileLocation());
        row.getPropertyEntries().add(image);

        if (cubeContainer.getExposedPorts() != null) {
            row.getPropertyEntries().add(cell("Exposed Ports", cubeContainer.getExposedPorts()));
        }

        if (cubeContainer.getPortBindings() != null) {
            row.getPropertyEntries().add(cell("Port Binding", cubeContainer.getPortBindings()));
        }

        if (cubeContainer.getLinks() != null)  {
            row.getPropertyEntries().add(cell("Links", cubeContainer.getLinks()));
        }

        if (cubeContainer.getVolumes() != null) {
            row.getPropertyEntries().add(cell("Volumes", cubeContainer.getVolumes()));
        }

        if (cubeContainer.getBinds() != null) {
            row.getPropertyEntries().add(cell("Binds", cubeContainer.getBinds()));
        }

        if (cubeContainer.getEntryPoint() != null) {
            row.getPropertyEntries().add(cell("Entrypoint", cubeContainer.getEntryPoint()));
        }

        if (cubeContainer.getNetworkMode() != null) {
            row.getPropertyEntries().add(cell("Network Mode", cubeContainer.getNetworkMode()));
        }

        return row;
    }

    private static KeyValueEntry cell(String key, String content) {
        return new KeyValueEntry(key, content);
    }

    private static KeyValueEntry cell(String key, Collection content) {
        return new KeyValueEntry(key, content.toString());
    }
}
