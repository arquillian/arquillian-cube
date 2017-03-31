package org.arquillian.cube.impl.model;

public interface CubeId {

    String getId();

    boolean isMatching(CubeId other);

}
