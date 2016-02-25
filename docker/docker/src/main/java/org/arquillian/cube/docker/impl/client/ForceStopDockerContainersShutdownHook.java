package org.arquillian.cube.docker.impl.client;

import org.arquillian.cube.docker.impl.docker.DockerClientExecutor;
import org.arquillian.cube.spi.Cube;
import org.arquillian.cube.spi.CubeRegistry;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.test.spi.event.suite.BeforeClass;

import java.util.List;

public class ForceStopDockerContainersShutdownHook {

    public void attachShutDownHookForceStopDcokerContainers(@Observes BeforeClass event, final DockerClientExecutor dockerClientExecutor, final CubeRegistry cubeRegistry) {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                final List<Cube<?>> cubes = cubeRegistry.getCubes();
                for (Cube cube : cubes) {
                    // If container is started, and we are exiting we need to stop it.
                    // Notice that in case of STARTORCONNECT and STARTORCONNECTANDLEAVE the state is PRE_RUNNING
                    // so they are not going to be stopped
                    if (Cube.State.STARTED.equals(cube.state())) {
                        dockerClientExecutor.stopContainer(cube.getId());
                        dockerClientExecutor.removeContainer(cube.getId());
                    }
                }

            }
        });
    }

}
