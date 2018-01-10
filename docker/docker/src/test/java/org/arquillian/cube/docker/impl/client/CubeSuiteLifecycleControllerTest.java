package org.arquillian.cube.docker.impl.client;

import com.github.dockerjava.api.model.Container;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.arquillian.cube.docker.impl.docker.DockerClientExecutor;
import org.arquillian.cube.spi.ConnectionMode;
import org.arquillian.cube.spi.CubeConfiguration;
import org.arquillian.cube.spi.event.CreateCube;
import org.arquillian.cube.spi.event.DestroyCube;
import org.arquillian.cube.spi.event.PreRunningCube;
import org.arquillian.cube.spi.event.StartCube;
import org.arquillian.cube.spi.event.StopCube;
import org.jboss.arquillian.container.spi.ContainerRegistry;
import org.jboss.arquillian.core.api.annotation.ApplicationScoped;
import org.jboss.arquillian.core.test.AbstractManagerTestBase;
import org.jboss.arquillian.test.spi.event.suite.AfterSuite;
import org.jboss.arquillian.test.spi.event.suite.BeforeSuite;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
    public void shouldParseEmptyAutostart() throws Exception {
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("autoStartContainers", "");
        parameters.put("dockerContainers", "a:\n  image: a\nb:\n  image: a\n");

        CubeConfiguration cubeConfiguration = CubeConfiguration.fromMap(new HashMap<String, String>());
        bind(ApplicationScoped.class, CubeConfiguration.class, cubeConfiguration);

        CubeDockerConfiguration dockerConfiguration = CubeDockerConfiguration.fromMap(parameters, null);
        bind(ApplicationScoped.class, CubeDockerConfiguration.class, dockerConfiguration);

        fire(new BeforeSuite());

        assertEventFired(CreateCube.class, 0);
        assertEventFired(StartCube.class, 0);
    }

    @Test
    public void shouldParseEmptyValuesAutostart() throws Exception {
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("autoStartContainers", " ,  ");
        parameters.put("dockerContainers", "a:\n  image: a\nb:\n  image: a\n");

        CubeConfiguration cubeConfiguration = CubeConfiguration.fromMap(new HashMap<String, String>());
        bind(ApplicationScoped.class, CubeConfiguration.class, cubeConfiguration);

        CubeDockerConfiguration dockerConfiguration = CubeDockerConfiguration.fromMap(parameters, null);
        bind(ApplicationScoped.class, CubeDockerConfiguration.class, dockerConfiguration);

        fire(new BeforeSuite());

        assertEventFired(CreateCube.class, 0);
        assertEventFired(StartCube.class, 0);
    }

    @Test
    public void shouldParseTrimAutostart() throws Exception {
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("autoStartContainers", "a , b ");
        parameters.put("dockerContainers", "a:\n  image: a\nb:\n  image: a\n");

        CubeConfiguration cubeConfiguration = CubeConfiguration.fromMap(new HashMap<String, String>());
        bind(ApplicationScoped.class, CubeConfiguration.class, cubeConfiguration);

        CubeDockerConfiguration dockerConfiguration = CubeDockerConfiguration.fromMap(parameters, null);
        bind(ApplicationScoped.class, CubeDockerConfiguration.class, dockerConfiguration);

        fire(new BeforeSuite());

        assertEventFired(CreateCube.class, 2);
        assertEventFired(StartCube.class, 2);
        assertEventFiredOnOtherThread(CreateCube.class);
        assertEventFiredOnOtherThread(StartCube.class);
    }

    @Test
    public void shouldCreateAndStartAutoContainersWhenNoAutoStartIsProvided() {
        Map<String, String> dockerData = new HashMap<String, String>();
        dockerData.put("dockerContainers", "a:\n  image: a\n  links:\n    - b:b\nb:\n  image: a\n");

        CubeConfiguration cubeConfiguration = CubeConfiguration.fromMap(new HashMap<String, String>());
        bind(ApplicationScoped.class, CubeConfiguration.class, cubeConfiguration);

        CubeDockerConfiguration dockerConfiguration = CubeDockerConfiguration.fromMap(dockerData, null);
        dockerConfiguration.setAutoStartContainers(new AutomaticResolutionLinksAutoStartParser(Arrays.asList("a"),
            dockerConfiguration.getDockerContainersContent()));
        bind(ApplicationScoped.class, CubeDockerConfiguration.class, dockerConfiguration);

        ContainerRegistry containerRegistry = mock(ContainerRegistry.class);
        List<org.jboss.arquillian.container.spi.Container> containers = new ArrayList<>();
        org.jboss.arquillian.container.spi.Container container = mock(org.jboss.arquillian.container.spi.Container.class);
        when(container.getName()).thenReturn("a");
        containers.add(container);
        when(containerRegistry.getContainers()).thenReturn(containers);

        bind(ApplicationScoped.class, ContainerRegistry.class, containerRegistry);

        fire(new BeforeSuite());
        assertEventFired(CreateCube.class, 1);
        assertEventFired(StartCube.class, 1);
    }

    @Test
    public void shouldCreateAndStartAutoContainersDefiningRegularExpressions() {
        Map<String, String> dockerData = new HashMap<String, String>();
        dockerData.put("autoStartContainers", "regexp:a(.*)");
        dockerData.put("dockerContainers", "a:\n  image: a\nab:\n  image: a\nx:\n" +
            "  image: a\n");

        CubeConfiguration cubeConfiguration = CubeConfiguration.fromMap(new HashMap<String, String>());
        bind(ApplicationScoped.class, CubeConfiguration.class, cubeConfiguration);

        CubeDockerConfiguration dockerConfiguration = CubeDockerConfiguration.fromMap(dockerData, null);
        bind(ApplicationScoped.class, CubeDockerConfiguration.class, dockerConfiguration);

        fire(new BeforeSuite());

        assertEventFired(CreateCube.class, 2);
        assertEventFired(StartCube.class, 2);
        assertEventFiredOnOtherThread(CreateCube.class);
        assertEventFiredOnOtherThread(StartCube.class);
    }

    @Test
    public void shouldCreateAndStartAutoContainers() {

        Map<String, String> dockerData = new HashMap<String, String>();
        dockerData.put("autoStartContainers", "a,b");
        dockerData.put("dockerContainers", "a:\n  image: a\nb:\n  image: a\n");

        CubeConfiguration cubeConfiguration = CubeConfiguration.fromMap(new HashMap<String, String>());
        bind(ApplicationScoped.class, CubeConfiguration.class, cubeConfiguration);

        CubeDockerConfiguration dockerConfiguration = CubeDockerConfiguration.fromMap(dockerData, null);
        bind(ApplicationScoped.class, CubeDockerConfiguration.class, dockerConfiguration);

        fire(new BeforeSuite());

        assertEventFired(CreateCube.class, 2);
        assertEventFired(StartCube.class, 2);
        assertEventFiredOnOtherThread(CreateCube.class);
        assertEventFiredOnOtherThread(StartCube.class);
    }

    @Test
    public void shouldStopAndDestroyAutoContainers() {

        Map<String, String> dockerData = new HashMap<String, String>();
        dockerData.put("autoStartContainers", "a,b");
        dockerData.put("dockerContainers", "a:\n  image: a\nb:\n  image: a\n");

        CubeConfiguration cubeConfiguration = CubeConfiguration.fromMap(new HashMap<String, String>());
        bind(ApplicationScoped.class, CubeConfiguration.class, cubeConfiguration);

        CubeDockerConfiguration dockerConfiguration = CubeDockerConfiguration.fromMap(dockerData, null);
        bind(ApplicationScoped.class, CubeDockerConfiguration.class, dockerConfiguration);

        fire(new AfterSuite());

        assertEventFired(StopCube.class, 2);
        assertEventFired(DestroyCube.class, 2);
        assertEventFiredOnOtherThread(StopCube.class);
        assertEventFiredOnOtherThread(DestroyCube.class);
    }

    @Test
    public void shouldUsePreRunningContainers() {
        Map<String, String> cubeData = new HashMap<String, String>();
        cubeData.put("connectionMode", ConnectionMode.STARTORCONNECT.name());

        Map<String, String> dockerData = new HashMap<String, String>();
        dockerData.put("autoStartContainers", "a,b");
        dockerData.put("dockerContainers", "a:\n  image: a\nb:\n  image: a\n");

        CubeConfiguration cubeConfiguration = CubeConfiguration.fromMap(cubeData);
        bind(ApplicationScoped.class, CubeConfiguration.class, cubeConfiguration);

        CubeDockerConfiguration dockerConfiguration = CubeDockerConfiguration.fromMap(dockerData, null);
        bind(ApplicationScoped.class, CubeDockerConfiguration.class, dockerConfiguration);

        Container container = mock(Container.class);
        when(container.getNames()).thenReturn(new String[] {"a"});
        when(executor.listRunningContainers()).thenReturn(Arrays.asList(container));
        bind(ApplicationScoped.class, DockerClientExecutor.class, executor);

        fire(new BeforeSuite());

        assertEventFired(CreateCube.class, 1);
        assertEventFired(StartCube.class, 1);
        assertEventFired(PreRunningCube.class, 1);
        assertEventFiredOnOtherThread(CreateCube.class);
        assertEventFiredOnOtherThread(StartCube.class);
        assertEventFiredOnOtherThread(PreRunningCube.class);
    }

    @Test
    public void shouldStartAContainerInStartOrConnectModeAndStopIt() {
        Map<String, String> cubeData = new HashMap<String, String>();
        cubeData.put("connectionMode", ConnectionMode.STARTORCONNECT.name());

        Map<String, String> dockerData = new HashMap<String, String>();
        dockerData.put("autoStartContainers", "a,b");
        dockerData.put("dockerContainers", "a:\n  image: a\nb:\n  image: a\n");

        CubeConfiguration cubeConfiguration = CubeConfiguration.fromMap(cubeData);
        bind(ApplicationScoped.class, CubeConfiguration.class, cubeConfiguration);

        CubeDockerConfiguration dockerConfiguration = CubeDockerConfiguration.fromMap(dockerData, null);
        bind(ApplicationScoped.class, CubeDockerConfiguration.class, dockerConfiguration);

        Container container = mock(Container.class);
        when(container.getNames()).thenReturn(new String[] {"alreadyrun"});
        when(executor.listRunningContainers()).thenReturn(Arrays.asList(container));
        bind(ApplicationScoped.class, DockerClientExecutor.class, executor);

        fire(new BeforeSuite());

        assertEventFired(CreateCube.class, 2);
        assertEventFired(StartCube.class, 2);
        assertEventFired(PreRunningCube.class, 0);
        assertEventFiredOnOtherThread(CreateCube.class);
        assertEventFiredOnOtherThread(StartCube.class);
    }

    @Test
    public void shouldStartAContainerInStartOrConnectAndLeaveModeAndNotStopIt() {
        Map<String, String> cubeData = new HashMap<String, String>();
        cubeData.put("connectionMode", ConnectionMode.STARTORCONNECTANDLEAVE.name());

        Map<String, String> dockerData = new HashMap<String, String>();
        dockerData.put("autoStartContainers", "a,b");
        dockerData.put("dockerContainers", "a:\n  image: a\nb:\n  image: a\n");

        CubeConfiguration cubeConfiguration = CubeConfiguration.fromMap(cubeData);
        bind(ApplicationScoped.class, CubeConfiguration.class, cubeConfiguration);

        CubeDockerConfiguration dockerConfiguration = CubeDockerConfiguration.fromMap(dockerData, null);
        bind(ApplicationScoped.class, CubeDockerConfiguration.class, dockerConfiguration);

        Container container = mock(Container.class);
        when(container.getNames()).thenReturn(new String[] {"alreadyrun"});
        when(executor.listRunningContainers()).thenReturn(Arrays.asList(container));
        bind(ApplicationScoped.class, DockerClientExecutor.class, executor);

        fire(new BeforeSuite());

        assertEventFired(CreateCube.class, 2);
        assertEventFired(StartCube.class, 2);
        assertEventFired(PreRunningCube.class, 2);
        assertEventFiredOnOtherThread(CreateCube.class);
        assertEventFiredOnOtherThread(StartCube.class);
        assertEventFiredOnOtherThread(PreRunningCube.class);
    }
}
