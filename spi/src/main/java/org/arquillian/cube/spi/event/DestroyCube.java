package org.arquillian.cube.spi.event;

import org.arquillian.cube.spi.Cube;

public class DestroyCube implements CubeControlEvent {

    private String cubeId;

    public DestroyCube(Cube cube) {
        this(cube.getId());
    }

    public DestroyCube(String cubeId) {
        this.cubeId = cubeId;
    }

    @Override
    public String getCubeId() {
        return cubeId;
    }
}
