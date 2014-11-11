package org.arquillian.cube.spi.event.lifecycle;

import org.jboss.arquillian.core.spi.event.Event;

public abstract class CubeLifecyleEvent implements Event {

    private String cubeId;

    public CubeLifecyleEvent(String cubeId) {
        this.cubeId = cubeId;
    }

    public String getCubeId() {
        return cubeId;
    }
}
