package org.arquillian.cube.impl.client;

import org.arquillian.cube.impl.util.ConfigUtil;
import org.arquillian.cube.spi.event.CreateCube;
import org.arquillian.cube.spi.event.CubeControlEvent;
import org.arquillian.cube.spi.event.DestroyCube;
import org.arquillian.cube.spi.event.StartCube;
import org.arquillian.cube.spi.event.StopCube;
import org.jboss.arquillian.core.api.Event;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.test.spi.event.suite.AfterSuite;
import org.jboss.arquillian.test.spi.event.suite.BeforeSuite;

public class CubeSuiteLifecycleController {

    @Inject
    private Event<CubeControlEvent> controlEvent;

    public void startAutoContainers(@Observes(precedence = 100) BeforeSuite event, CubeConfiguration configuration) {
        for(String cubeId : configuration.getAutoStartContainers()) {
            controlEvent.fire(new CreateCube(cubeId));
            controlEvent.fire(new StartCube(cubeId));
        }
    }

    public void stopAutoContainers(@Observes(precedence = -100) AfterSuite event, CubeConfiguration configuration) {
        for(String cubeId : ConfigUtil.reverse(configuration.getAutoStartContainers())) {
            controlEvent.fire(new StopCube(cubeId));
            controlEvent.fire(new DestroyCube(cubeId));
        }
    }
}
