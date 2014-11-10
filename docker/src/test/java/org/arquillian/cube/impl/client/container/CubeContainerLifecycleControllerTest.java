package org.arquillian.cube.impl.client.container;

import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;

import org.arquillian.cube.impl.model.DockerCubeRegistry;
import org.arquillian.cube.spi.Cube;
import org.arquillian.cube.spi.CubeRegistry;
import org.arquillian.cube.spi.events.CreateCube;
import org.arquillian.cube.spi.events.DestroyCube;
import org.arquillian.cube.spi.events.StartCube;
import org.arquillian.cube.spi.events.StopCube;
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
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CubeContainerLifecycleControllerTest extends AbstractManagerTestBase {

    @Override
    protected void addExtensions(List<Class<?>> extensions) {
        extensions.add(CubeContainerLifecycleController.class);
        super.addExtensions(extensions);
    }

    public static final String CUBE_ID = "test";
    public static final String MISSING_CUBE_ID = "_MISSING_";

    @Mock
    private Cube cube;

    @Mock
    private Container container;

    @SuppressWarnings("rawtypes")
    @Mock
    private DeployableContainer deployableContainer;

    @SuppressWarnings("rawtypes")
    @Mock
    private DeployableContainer deployableContainerNoMatch;

    @Mock
    private ContainerRegistry containerRegistry;

    private CubeRegistry registry;

    @Before
    @SuppressWarnings("unchecked")
    public void setup() {
        when(cube.getId()).thenReturn(CUBE_ID);
        when(container.getName()).thenReturn(CUBE_ID);
        when(container.getDeployableContainer()).thenReturn(deployableContainer);
        when(containerRegistry.getContainers()).thenReturn(Arrays.asList(container));
        registry = new DockerCubeRegistry();
        registry.addCube(cube);

        bind(ApplicationScoped.class, CubeRegistry.class, registry);
        bind(ApplicationScoped.class, ContainerRegistry.class, containerRegistry);
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
}
