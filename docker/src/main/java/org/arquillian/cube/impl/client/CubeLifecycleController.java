package org.arquillian.cube.impl.client;

import org.arquillian.cube.spi.Cube;
import org.arquillian.cube.spi.CubeRegistry;
import org.arquillian.cube.spi.events.CreateCube;
import org.arquillian.cube.spi.events.DestroyCube;
import org.arquillian.cube.spi.events.StartCube;
import org.arquillian.cube.spi.events.StopCube;
import org.jboss.arquillian.core.api.annotation.Observes;

public class CubeLifecycleController {

    public void create(@Observes CreateCube event, CubeRegistry registry) {
        validateAndGet(registry, event.getCubeId()).create();
    }

    public void start(@Observes StartCube event, CubeRegistry registry) {
        validateAndGet(registry, event.getCubeId()).start();
    }

    public void start(@Observes StopCube event, CubeRegistry registry) {
        validateAndGet(registry, event.getCubeId()).stop();
    }

    public void start(@Observes DestroyCube event, CubeRegistry registry) {
        validateAndGet(registry, event.getCubeId()).destroy();
    }

    private Cube validateAndGet(CubeRegistry registry, String cubeId) {
        Cube cube = registry.getCube(cubeId);
        if(cube == null) {
            throw new IllegalArgumentException("No cube with id " + cubeId + " found in registry");
        }
        return cube;
    }
}