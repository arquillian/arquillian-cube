package org.arquillian.cube.docker.impl.client.reporter;

import org.jboss.arquillian.core.spi.LoadableExtension;

public class CubeDockerRecorderExtension implements LoadableExtension {

    @Override
    public void register(ExtensionBuilder builder) {

        // Only if recorder-reporter is in classpath we should provide reporting capabilities.
        if (Validate.classExists("org.arquillian.recorder.reporter.ReporterExtension")) {
            builder.observer(TakeDockerEnvironment.class);
        }

        // Only if drone is in classpath we should provide reporting capabilities for videos.
        if (Validate.classExists("org.arquillian.cube.docker.drone.CubeDockerDroneExtension")) {
            builder.observer(TakeVncDroneVideo.class);
        }

    }
}
