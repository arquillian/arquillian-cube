package org.arquillian.cube.openshift.impl.client;

import org.arquillian.cube.kubernetes.api.Configuration;
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

public class OpenShiftSuiteLifecycleController {

    @Inject
    private Event<CubeControlEvent> controlEvent;

    public void startAutoContainers(@Observes(precedence = 99) BeforeSuite event,
        Configuration conf) {
        if (!(conf instanceof CubeOpenShiftConfiguration)) {
            return;
        }
        CubeOpenShiftConfiguration openshiftConfiguration = (CubeOpenShiftConfiguration) conf;

        for (String cubeId : openshiftConfiguration.getAutoStartContainers()) {
            controlEvent.fire(new CreateCube(cubeId));
            controlEvent.fire(new StartCube(cubeId));
        }
    }

    public void stopAutoContainers(@Observes(precedence = -99) AfterSuite event,
        Configuration conf) {
        if (!(conf instanceof CubeOpenShiftConfiguration)) {
            return;
        }
        CubeOpenShiftConfiguration openshiftConfiguration = (CubeOpenShiftConfiguration) conf;

        String[] autostart = openshiftConfiguration.getAutoStartContainers();
        for (int i = autostart.length - 1; i > -1; i--) {
            String cubeId = autostart[i];
            controlEvent.fire(new StopCube(cubeId));
            controlEvent.fire(new DestroyCube(cubeId));
        }
    }
}
