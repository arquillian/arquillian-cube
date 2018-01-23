package org.arquillian.cube.impl.client;

import java.util.List;
import org.arquillian.cube.spi.Cube;
import org.arquillian.cube.spi.CubeRegistry;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.test.spi.event.suite.BeforeSuite;

public class ForceStopDockerContainersShutdownHook {

    public void attachShutDownHookForceStopDockerContainers(@Observes(precedence = 200) BeforeSuite event,
        final CubeRegistry cubeRegistry) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            final List<Cube<?>> cubes = cubeRegistry.getCubes();
            for (Cube cube : cubes) {

                    cube.stop();
                    cube.destroy();
            }
        }));
    }
}
