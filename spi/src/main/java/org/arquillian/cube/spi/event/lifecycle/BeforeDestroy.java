package org.arquillian.cube.spi.event.lifecycle;

public class BeforeDestroy extends CubeLifecyleEvent {

    public BeforeDestroy(String cubeId) {
        super(cubeId);
    }
}
