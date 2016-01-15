package org.arquillian.cube.spi.event;

import org.arquillian.cube.spi.Cube;

public class StartCube extends CubeControlEvent {

    public StartCube(Cube<?> cube) {
        this(cube.getId());
    }

    public StartCube(String cubeId) {
        super(cubeId);
    }
}
