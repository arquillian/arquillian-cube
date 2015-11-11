package org.arquillian.cube.spi;

import java.util.List;

public interface CubeRegistry {

    void addCube(Cube cube);

    Cube getCube(String id);

    void removeCube(String id);

    List<Cube> getByMetadata(Class<?> metadata);

    List<Cube> getCubes();
}
