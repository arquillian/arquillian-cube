package org.arquillian.cube.spi.event.lifecycle;

public class AfterDestroy extends CubeLifecyleEvent {

    public AfterDestroy(String cubeId) {
        super(cubeId);
    }
}
