package org.arquillian.cube.spi.event.lifecycle;

public class AfterCreate extends CubeLifecyleEvent {

    public AfterCreate(String cubeId) {
        super(cubeId);
    }
}
