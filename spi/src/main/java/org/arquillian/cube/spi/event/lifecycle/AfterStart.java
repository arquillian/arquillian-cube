package org.arquillian.cube.spi.event.lifecycle;

public class AfterStart extends CubeLifecyleEvent {

    public AfterStart(String cubeId) {
        super(cubeId);
    }
}
