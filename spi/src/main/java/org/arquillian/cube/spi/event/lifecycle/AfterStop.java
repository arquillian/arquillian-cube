package org.arquillian.cube.spi.event.lifecycle;

public class AfterStop extends CubeLifecyleEvent {

    public AfterStop(String cubeId) {
        super(cubeId);
    }
}
