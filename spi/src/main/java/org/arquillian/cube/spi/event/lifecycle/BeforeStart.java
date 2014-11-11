package org.arquillian.cube.spi.event.lifecycle;

public class BeforeStart extends CubeLifecyleEvent {

    public BeforeStart(String cubeId) {
        super(cubeId);
    }
}
