package org.arquillian.cube.spi;

import java.util.List;
import org.arquillian.cube.spi.metadata.CubeMetadata;

public interface CubeRegistry {

    void addCube(Cube<?> cube);

    Cube<?> getCube(String id);

    <T extends Cube<?>> T getCube(String id, Class<T> type);

    void removeCube(String id);

    List<Cube<?>> getByMetadata(Class<? extends CubeMetadata> metadata);

    List<Cube<?>> getCubes();
}
