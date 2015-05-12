package org.arquillian.cube.impl.client;

import org.arquillian.cube.spi.Cube;
import org.arquillian.cube.spi.CubeRegistry;
import org.arquillian.cube.spi.event.CreateCube;
import org.arquillian.cube.spi.event.DestroyCube;
import org.arquillian.cube.spi.event.PreRunningCube;
import org.arquillian.cube.spi.event.StartCube;
import org.arquillian.cube.spi.event.StopCube;
import org.jboss.arquillian.core.api.annotation.Observes;

public class CubeLifecycleController {

    public void create(@Observes CreateCube event, CubeRegistry registry) {
        validateAndGet(registry, event.getCubeId()).create();
    }

    public void start(@Observes StartCube event, CubeRegistry registry) {
        validateAndGet(registry, event.getCubeId()).start();
    }

    public void stop(@Observes StopCube event, CubeRegistry registry) {
        validateAndGet(registry, event.getCubeId()).stop();
    }

    public void destroy(@Observes DestroyCube event, CubeRegistry registry) {
        validateAndGet(registry, event.getCubeId()).destroy();
    }

    public void changeToPreRunning(@Observes PreRunningCube event, CubeRegistry registry) {
        validateAndGet(registry, event.getCubeId()).changeToPreRunning();
    }

    private Cube validateAndGet(CubeRegistry registry, String cubeId) {
        Cube cube = registry.getCube(cubeId);
        if(cube == null) {
            throw new IllegalArgumentException("No cube with id " + cubeId + " found in registry");
        }
        return cube;
    }
}