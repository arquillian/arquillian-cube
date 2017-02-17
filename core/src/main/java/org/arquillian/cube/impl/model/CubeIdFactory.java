package org.arquillian.cube.impl.model;

import java.util.Objects;

public class CubeIdFactory {
    private static final CubeIdFactory INSTANCE = new CubeIdFactory();

    public static CubeIdFactory get() {
        return INSTANCE;
    }

    public CubeId create(String id) {
        Objects.requireNonNull(id, "Id must not be null.");
        final CubeId cubeId;

        if (id.matches(StarredCubeId.PATTERN)) {
            cubeId = new StarredCubeId(id);
        } else if (id.matches(ParallelizedCubeId.PATTERN)) {
            cubeId = new ParallelizedCubeId(id);
        } else {
            cubeId = new DefaultCubeId(id);
        }

        return cubeId;
    }
}
