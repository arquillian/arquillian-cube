package org.arquillian.cube.impl.client;

import org.arquillian.cube.spi.Cube;
import org.arquillian.cube.spi.CubeRegistry;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.test.spi.event.suite.BeforeSuite;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ForceStopDockerContainersShutdownHook {

    public void attachShutDownHookForceStopDockerContainers(@Observes(precedence = 200) BeforeSuite event,
                                                            final CubeRegistry cubeRegistry) {

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            final List<Cube<?>> cubes = cubeRegistry.getCubes();
            for (Cube cube : cubes) {

                try {
                    cube.stop();
                } catch (final Throwable e) {
                    Logger.getLogger(ForceStopDockerContainersShutdownHook.class.getName()).log(Level.WARNING, "Failed to stop container: " + cube.getId(), e);
                } finally {
                    try {
                        cube.destroy();
                    } catch (final Throwable ignore) {
                        //no-op
                    }
                }

            }
        }));
    }
}
