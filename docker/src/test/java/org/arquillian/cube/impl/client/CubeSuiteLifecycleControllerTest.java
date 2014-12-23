package org.arquillian.cube.impl.client;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.arquillian.cube.impl.docker.DockerClientExecutor;
import org.arquillian.cube.spi.event.CreateCube;
import org.arquillian.cube.spi.event.DestroyCube;
import org.arquillian.cube.spi.event.PreRunningCube;
import org.arquillian.cube.spi.event.StartCube;
import org.arquillian.cube.spi.event.StopCube;
import org.jboss.arquillian.core.api.annotation.ApplicationScoped;
import org.jboss.arquillian.core.test.AbstractManagerTestBase;
import org.jboss.arquillian.test.spi.event.suite.AfterSuite;
import org.jboss.arquillian.test.spi.event.suite.BeforeSuite;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.github.dockerjava.api.model.Container;

@RunWith(MockitoJUnitRunner.class)
public class CubeSuiteLifecycleControllerTest extends AbstractManagerTestBase {

    @Mock
    private DockerClientExecutor executor;

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

    @Test
    public void shouldUsePreRunningContainers() {
        Map<String, String> data = new HashMap<String, String>();
        data.put("autoStartContainers", "a,b");
        data.put("shouldAllowToConnectToRunningContainers", "true");
        
        CubeConfiguration configuration = CubeConfiguration.fromMap(data);
        bind(ApplicationScoped.class, CubeConfiguration.class, configuration);
        Container container = mock(Container.class);
        when(container.getNames()).thenReturn(new String[]{"a"});
        when(executor.listRunningContainers()).thenReturn(Arrays.asList(container));
        bind(ApplicationScoped.class, DockerClientExecutor.class, executor);

        fire(new BeforeSuite());

        assertEventFired(CreateCube.class, 1);
        assertEventFired(StartCube.class, 1);
        assertEventFired(PreRunningCube.class, 1);
    }
}
