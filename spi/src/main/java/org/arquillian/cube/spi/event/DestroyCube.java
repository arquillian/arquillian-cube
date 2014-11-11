package org.arquillian.cube.spi.event;

import org.arquillian.cube.spi.Cube;

public class DestroyCube extends CubeControlEvent {

    public DestroyCube(Cube cube) {
        this(cube.getId());
    }

    public DestroyCube(String cubeId) {
        super(cubeId);
    }
}
