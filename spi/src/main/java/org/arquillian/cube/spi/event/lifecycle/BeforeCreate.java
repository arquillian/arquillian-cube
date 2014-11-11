package org.arquillian.cube.spi.event.lifecycle;

public class BeforeCreate extends CubeLifecyleEvent {

    public BeforeCreate(String cubeId) {
        super(cubeId);
    }
}
