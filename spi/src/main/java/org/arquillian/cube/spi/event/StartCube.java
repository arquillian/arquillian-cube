package org.arquillian.cube.spi.event;

import org.arquillian.cube.spi.Cube;

public class StartCube implements CubeControlEvent {

    private String cubeId;

    public StartCube(Cube cube) {
        this(cube.getId());
    }

    public StartCube(String cubeId) {
        this.cubeId = cubeId;
    }

    @Override
    public String getCubeId() {
        return cubeId;
    }
}
