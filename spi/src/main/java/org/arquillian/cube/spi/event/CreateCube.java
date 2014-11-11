package org.arquillian.cube.spi.event;

import org.arquillian.cube.spi.Cube;

public class CreateCube extends CubeControlEvent {

    public CreateCube(Cube cube) {
        this(cube.getId());
    }

    public CreateCube(String cubeId) {
        super(cubeId);
    }
}
