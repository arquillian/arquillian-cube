package org.arquillian.cube.spi.event;

import org.jboss.arquillian.core.spi.event.Event;

public interface CubeControlEvent extends Event {

    public String getCubeId();
}
