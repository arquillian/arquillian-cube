package org.arquillian.cube.spi.event.lifecycle;

public class BeforeStop extends CubeLifecyleEvent {

    public BeforeStop(String cubeId) {
        super(cubeId);
    }
}
