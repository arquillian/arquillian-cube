package org.arquillian.cube.impl.client;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.arquillian.cube.spi.event.CreateCube;
import org.arquillian.cube.spi.event.DestroyCube;
import org.arquillian.cube.spi.event.StartCube;
import org.arquillian.cube.spi.event.StopCube;
import org.jboss.arquillian.core.api.annotation.ApplicationScoped;
import org.jboss.arquillian.core.test.AbstractManagerTestBase;
import org.jboss.arquillian.test.spi.event.suite.AfterSuite;
import org.jboss.arquillian.test.spi.event.suite.BeforeSuite;
import org.junit.Test;

public class CubeSuiteLifecycleControllerTest extends AbstractManagerTestBase {

    @Override
    protected void addExtensions(List<Class<?>> extensions) {
        extensions.add(CubeSuiteLifecycleController.class);
        super.addExtensions(extensions);
    }

    @Test
    public void shouldCreateAndStartAutoContainers() {

        Map<String, String> data = new HashMap<String, String>();
        data.put("autoStartContainers", "a,b");

        CubeConfiguration configuration = CubeConfiguration.fromMap(data);
        bind(ApplicationScoped.class, CubeConfiguration.class, configuration);

        fire(new BeforeSuite());

        assertEventFired(CreateCube.class, 2);
        assertEventFired(StartCube.class, 2);
    }

    @Test
    public void shouldStopAndDestroyAutoContainers() {

        Map<String, String> data = new HashMap<String, String>();
        data.put("autoStartContainers", "a,b");

        CubeConfiguration configuration = CubeConfiguration.fromMap(data);
        bind(ApplicationScoped.class, CubeConfiguration.class, configuration);

        fire(new AfterSuite());

        assertEventFired(StopCube.class, 2);
        assertEventFired(DestroyCube.class, 2);
    }
}
