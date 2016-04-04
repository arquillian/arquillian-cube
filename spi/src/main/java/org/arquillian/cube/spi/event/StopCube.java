package org.arquillian.cube.spi.event;

import org.arquillian.cube.spi.Cube;

public class StopCube extends CubeControlEvent {

    public StopCube(Cube<?> cube) {
        this(cube.getId());
    }

    public StopCube(String cubeId) {
        super(cubeId);
    }
}
