package org.arquillian.cube.spi;

import java.util.List;

public interface CubeRegistry {

    void addCube(Cube cube);

    Cube getCube(String id);

    List<Cube> getCubes();
}
