package org.arquillian.cube.spi.event;

import org.arquillian.cube.spi.Cube;

public class CreateCube implements CubeControlEvent {

    private String cubeId;

    public CreateCube(Cube cube) {
        this(cube.getId());
    }

    public CreateCube(String cubeId) {
        this.cubeId = cubeId;
    }

    @Override
    public String getCubeId() {
        return cubeId;
    }
}
