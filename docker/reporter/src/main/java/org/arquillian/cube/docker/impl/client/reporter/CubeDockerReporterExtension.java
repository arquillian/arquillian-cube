package org.arquillian.cube.docker.impl.client.reporter;

import org.arquillian.reporter.api.model.StringKey;
import org.jboss.arquillian.core.spi.LoadableExtension;

public class CubeDockerReporterExtension implements LoadableExtension {

    @Override
    public void register(ExtensionBuilder builder) {

        // Only if reporter is in classpath we should provide reporting capabilities.
        final boolean reportedInClasspath = Validate.classExists("org.arquillian.core.reporter.ArquillianCoreReporterExtension");
        if (reportedInClasspath) {
            builder.observer(TakeDockerEnvironment.class);
            builder.service(StringKey.class, DockerEnvironmentReportKey.class);
        }

        // Only if drone is in classpath we should provide reporting capabilities for videos.
        if (reportedInClasspath && Validate.classExists("org.arquillian.cube.docker.drone.CubeDockerDroneExtension")) {
            builder.observer(TakeVncDroneVideo.class);
        }

        // Only if restassured is in classpath we should provide reporting capabilities for restassured
        if (reportedInClasspath && Validate.classExists("org.arquillian.cube.docker.restassured.RestAssuredExtension")) {
            builder.observer(TakeRestAssuredContent.class);
        }

    }
}
