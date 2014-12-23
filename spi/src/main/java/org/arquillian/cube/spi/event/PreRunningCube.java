package org.arquillian.cube.spi.event;

import org.arquillian.cube.spi.Cube;

public class PreRunningCube extends CubeControlEvent {

    public PreRunningCube(String cubeId) {
        super(cubeId);
    }

    public PreRunningCube(Cube cube) {
        this(cube.getId());
    }
}
