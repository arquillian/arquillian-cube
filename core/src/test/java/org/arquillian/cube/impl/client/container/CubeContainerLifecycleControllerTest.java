package org.arquillian.cube.impl.client.container;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.arquillian.cube.impl.model.LocalCubeRegistry;
import org.arquillian.cube.spi.ConnectionMode;
import org.arquillian.cube.spi.Cube;
import org.arquillian.cube.spi.CubeConfiguration;
import org.arquillian.cube.spi.CubeRegistry;
import org.arquillian.cube.spi.event.CreateCube;
import org.arquillian.cube.spi.event.DestroyCube;
import org.arquillian.cube.spi.event.PreRunningCube;
import org.arquillian.cube.spi.event.StartCube;
import org.arquillian.cube.spi.event.StopCube;
import org.jboss.arquillian.config.descriptor.api.ContainerDef;
import org.jboss.arquillian.container.spi.Container;
import org.jboss.arquillian.container.spi.ContainerRegistry;
import org.jboss.arquillian.container.spi.client.container.DeployableContainer;
import org.jboss.arquillian.container.spi.event.container.AfterStop;
import org.jboss.arquillian.container.spi.event.container.BeforeStart;
import org.jboss.arquillian.core.api.annotation.ApplicationScoped;
import org.jboss.arquillian.core.test.AbstractManagerTestBase;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CubeContainerLifecycleControllerTest extends AbstractManagerTestBase {

    public static final String CUBE_ID = "test";
    public static final String MISSING_CUBE_ID = "_MISSING_";
    @Mock
    private Cube<?> cube;
    @Mock
    private Container container;
    @Mock
    private ContainerDef containerDef;
    @SuppressWarnings("rawtypes")
    @Mock
    private DeployableContainer deployableContainer;
    @SuppressWarnings("rawtypes")
    @Mock
    private DeployableContainer deployableContainerNoMatch;
    @Mock
    private ContainerRegistry containerRegistry;
    private CubeRegistry registry;

    @Override
    protected void addExtensions(List<Class<?>> extensions) {
        extensions.add(CubeContainerLifecycleController.class);
        super.addExtensions(extensions);
    }

    @Before
    @SuppressWarnings("unchecked")
    public void setup() {
        when(cube.getId()).thenReturn(CUBE_ID);
        when(container.getName()).thenReturn(CUBE_ID);
        when(container.getDeployableContainer()).thenReturn(deployableContainer);
        when(container.getContainerConfiguration()).thenReturn(containerDef);
        when(containerDef.getContainerProperties()).thenReturn(Collections.EMPTY_MAP);
        when(containerRegistry.getContainers()).thenReturn(Arrays.asList(container));
        registry = new LocalCubeRegistry();
        registry.addCube(cube);

        bind(ApplicationScoped.class, CubeRegistry.class, registry);
        bind(ApplicationScoped.class, ContainerRegistry.class, containerRegistry);
        bind(ApplicationScoped.class, CubeConfiguration.class, new CubeConfiguration());
    }

    @Test
    public void shouldUsePreRunningContainerInStartOrConnectMode() {
        Map<String, String> data = new HashMap<String, String>();
        data.put("connectionMode", ConnectionMode.STARTORCONNECT.name());
        bind(ApplicationScoped.class, CubeConfiguration.class, CubeConfiguration.fromMap(data));

        when(cube.isRunningOnRemote()).thenReturn(true);

        fire(new BeforeStart(deployableContainer));
        assertEventFired(PreRunningCube.class, 1);
    }

    @Test
    public void shouldStartAContainerInStartOrConnectModeAndStopIt() {
        Map<String, String> data = new HashMap<String, String>();
        data.put("connectionMode", ConnectionMode.STARTORCONNECT.name());
        bind(ApplicationScoped.class, CubeConfiguration.class, CubeConfiguration.fromMap(data));

        when(cube.isRunningOnRemote()).thenReturn(false);

        fire(new BeforeStart(deployableContainer));
        assertEventFired(CreateCube.class, 1);
        assertEventFired(StartCube.class, 1);
        assertEventFired(PreRunningCube.class, 0);
    }

    @Test
    public void shouldStartAContainerInStartOrConnectLeaveModeAndNotStopIt() {
        Map<String, String> data = new HashMap<String, String>();
        data.put("connectionMode", ConnectionMode.STARTORCONNECTANDLEAVE.name());
        bind(ApplicationScoped.class, CubeConfiguration.class, CubeConfiguration.fromMap(data));

        when(cube.isRunningOnRemote()).thenReturn(false);

        fire(new BeforeStart(deployableContainer));
        assertEventFired(CreateCube.class, 1);
        assertEventFired(StartCube.class, 1);
        assertEventFired(PreRunningCube.class, 1);
    }

    @Test
    public void shouldCreateAndStartCubeDuringBeforeStart() {
        fire(new BeforeStart(deployableContainer));
        assertEventFired(CreateCube.class, 1);
        assertEventFired(StartCube.class, 1);
    }

    @Test
    public void shouldStopAndDestroyCubeDuringAfterStop() {
        fire(new AfterStop(deployableContainer));
        assertEventFired(StopCube.class, 1);
        assertEventFired(DestroyCube.class, 1);
    }

    @Test
    public void shouldNotCreateAndStartWhenNoContainerFound() {
        fire(new BeforeStart(deployableContainerNoMatch));
        assertEventFired(CreateCube.class, 0);
        assertEventFired(StartCube.class, 0);
    }

    @Test
    public void shouldNotStopAndDestroyWhenNoContainerFound() {
        fire(new AfterStop(deployableContainerNoMatch));
        assertEventFired(StopCube.class, 0);
        assertEventFired(DestroyCube.class, 0);
    }

    @Test
    public void shouldNotCreateAndStartWhenNoCubeFoundMatchingContainer() {
        when(container.getName()).thenReturn(MISSING_CUBE_ID);
        fire(new BeforeStart(deployableContainer));
        assertEventFired(CreateCube.class, 0);
        assertEventFired(StartCube.class, 0);
    }

    @Test
    public void shouldNotStopAndDestroyWhenNoCubeFoundMatchingContainer() {
        when(container.getName()).thenReturn(MISSING_CUBE_ID);
        fire(new AfterStop(deployableContainer));
        assertEventFired(StopCube.class, 0);
        assertEventFired(DestroyCube.class, 0);
    }

    @Test
    public void shouldUseOverriddenCubeId() {
        Map<String, String> containerConfig = new HashMap<String, String>();
        containerConfig.put("cubeId", CUBE_ID);

        when(container.getName()).thenReturn(MISSING_CUBE_ID);
        when(containerDef.getContainerProperties()).thenReturn(containerConfig);

        shouldCreateAndStartCubeDuringBeforeStart();
    }
}
