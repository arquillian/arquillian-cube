package org.arquillian.cube.spi.event;

import org.arquillian.cube.spi.Cube;

public class StopCube implements CubeControlEvent {

    private String cubeId;

    public StopCube(Cube cube) {
        this(cube.getId());
    }

    public StopCube(String cubeId) {
        this.cubeId = cubeId;
    }

    @Override
    public String getCubeId() {
        return cubeId;
    }
}
