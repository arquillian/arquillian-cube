package org.arquillian.cube.docker.impl.client.metadata;

import static org.arquillian.cube.impl.reporter.DockerReportKey.*;

import org.arquillian.cube.docker.impl.client.config.CubeContainer;
import org.arquillian.cube.docker.impl.model.DockerCube;
import org.arquillian.cube.spi.Cube;
import org.arquillian.cube.spi.metadata.CanReportMetrics;
import org.arquillian.reporter.api.builder.Reporter;
import org.arquillian.reporter.api.builder.report.ReportBuilder;
import org.arquillian.reporter.api.builder.report.ReportInSectionBuilder;
import org.arquillian.reporter.api.event.TestSuiteConfigurationSection;
import org.arquillian.reporter.api.model.report.ConfigurationReport;

import java.util.EnumSet;

/**
 * Reporting metrics capabilities for Docker Cube.
 */
public class ReportMetrics implements CanReportMetrics {

    private DockerCube dockerCube;

    public ReportMetrics(DockerCube dockerCube) {
        this.dockerCube = dockerCube;
    }

    @Override
    public ReportInSectionBuilder report() {

        boolean error = EnumSet.of(Cube.State.START_FAILED, Cube.State.CREATE_FAILED,
                Cube.State.STOP_FAILED, Cube.State.DESTORY_FAILED)
                .contains(dockerCube.state());

        final ReportBuilder reportBuilder = Reporter.createReport(new ConfigurationReport(CONTAINER_SECTION_NAME))
                .addKeyValueEntry(CONTAINER_NAME, dockerCube.getId())
                .addKeyValueEntry(ERROR_DURING_LIFECYCLE, Boolean.toString(error))
                .addKeyValueEntry(STARTING_TIME, String.format("%s ms", dockerCube.getStartingTimeInMillis()))
                .addKeyValueEntry(STOPPING_TIME, String.format("%s ms", dockerCube.getStoppingTimeInMillis()));

        final CubeContainer configuration = dockerCube.configuration();

        if (configuration.getImage() != null) {
            reportBuilder.addKeyValueEntry(IMAGE_NAME, configuration.getImage().toString());
        } else {
            reportBuilder.addKeyValueEntry(BUILD_LOCATION, configuration.getBuildImage().getDockerfileLocation());
        }

        if (configuration.getExposedPorts() != null) {
            reportBuilder.addKeyValueEntry(EXPOSED_PORTS, configuration.getExposedPorts().toString());
        }

        if (configuration.getPortBindings() != null) {
            reportBuilder.addKeyValueEntry(PORT_BINDING, configuration.getPortBindings().toString());
        }

        if (configuration.getLinks() != null)  {
            reportBuilder.addKeyValueEntry(LINKS, configuration.getLinks().toString());
        }

        if (configuration.getVolumes() != null) {
            reportBuilder.addKeyValueEntry(VOLUMES, configuration.getVolumes().toString());
        }

        if (configuration.getBinds() != null) {
            reportBuilder.addKeyValueEntry(BINDS, configuration.getBinds().toString());
        }

        if (configuration.getEntryPoint() != null) {
            reportBuilder.addKeyValueEntry(ENTRY_POINT, configuration.getEntryPoint().toString());
        }

        if (configuration.getNetworkMode() != null) {
            reportBuilder.addKeyValueEntry(NETWORK_MODE, configuration.getNetworkMode());
        }

        return reportBuilder.inSection(new TestSuiteConfigurationSection(dockerCube.getId()));

    }

}
