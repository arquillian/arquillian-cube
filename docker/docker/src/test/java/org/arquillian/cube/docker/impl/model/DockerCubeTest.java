package org.arquillian.cube.docker.impl.model;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectContainerCmd;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Ports;
import org.arquillian.cube.docker.impl.client.config.CubeContainer;
import org.arquillian.cube.docker.impl.docker.DockerClientExecutor;
import org.arquillian.cube.spi.event.lifecycle.AfterCreate;
import org.arquillian.cube.spi.event.lifecycle.AfterDestroy;
import org.arquillian.cube.spi.event.lifecycle.AfterStart;
import org.arquillian.cube.spi.event.lifecycle.AfterStop;
import org.arquillian.cube.spi.event.lifecycle.BeforeCreate;
import org.arquillian.cube.spi.event.lifecycle.BeforeDestroy;
import org.arquillian.cube.spi.event.lifecycle.BeforeStart;
import org.arquillian.cube.spi.event.lifecycle.BeforeStop;
import org.jboss.arquillian.core.api.Injector;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.test.AbstractManagerTestBase;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DockerCubeTest extends AbstractManagerTestBase {

    private static final String ID = "test";

    @Mock
    private DockerClientExecutor executor;

    @Mock
    private DockerClient dockerClient;

    @Mock
    private InspectContainerCmd inspectContainerCmd;

    @Mock
    private InspectContainerResponse inspectContainerResponse;

    @Inject
    private Instance<Injector> injectorInst;

    private DockerCube cube;

    @Before
    public void setup() {
        HostConfig hostConfig = new HostConfig();
        hostConfig.withPortBindings(new Ports());
        when(inspectContainerResponse.getHostConfig()).thenReturn(hostConfig);
        when(inspectContainerCmd.exec()).thenReturn(inspectContainerResponse);
        when(dockerClient.inspectContainerCmd(anyString())).thenReturn(inspectContainerCmd);
        when(executor.getDockerClient()).thenReturn(dockerClient);
        CubeContainer cubeContainer = new CubeContainer();
        cubeContainer.setRemoveVolumes(false);
        cube = injectorInst.get().inject(new DockerCube(ID, cubeContainer, executor));
    }

    @Test
    public void shouldFireLifecycleEventsDuringCreate() {
        cube.create();
        assertEventFired(BeforeCreate.class, 1);
        assertEventFired(AfterCreate.class, 1);
    }

    @Test
    public void shouldFireLifecycleEventsDuringCreateAfterDestroyed() {
        // given calling entire lifecycle to destroy cube
        createStartStopCube();
        cube.destroy();

        // when
        cube.create();

        // then event count is 2 which is for two cube.create()
        assertEventFired(BeforeCreate.class, 2);
        assertEventFired(AfterCreate.class, 2);
    }


    @Test
    public void shouldFireLifecycleEventsDuringStart() {
        cube.start();
        assertEventFired(BeforeStart.class, 1);
        assertEventFired(AfterStart.class, 1);
    }

    @Test
    public void shouldFireLifecycleEventsDuringStop() {
        createStartStopCube();
        assertEventFired(BeforeStop.class, 1);
        assertEventFired(AfterStop.class, 1);
    }

    @Test
    public void shouldFireLifecycleEventsDuringStopWhenContainerNotFound() {
        doThrow(new NotFoundException("container not found"))
            .when(executor).stopContainer(ID);
        createStartStopCube();
        assertEventFired(BeforeStop.class, 1);
        assertEventFired(AfterStop.class, 1);
    }

    @Test
    public void shouldFireLifecycleEventsDuringDestroy() {
        createStartStopCube(); // require a stopped Cube to destroy it.
        cube.destroy();
        assertEventFired(BeforeDestroy.class, 1);
        assertEventFired(AfterDestroy.class, 1);
    }

    @Test
    public void shouldFireLifecycleEventsDuringDestroyWhenContainerNotFound() {
        doThrow(new NotFoundException("container not found"))
            .when(executor).removeContainer(ID, false);
        createStartStopCube();
        cube.destroy();
        assertEventFired(BeforeDestroy.class, 1);
        assertEventFired(AfterDestroy.class, 1);
    }

    @Test
    public void shouldNotFireLifecycleEventsIfTryingToStopAlreadyDestroyedCube() {
        // given
        createStartStopCube();
        cube.destroy();

        // when
        cube.stop();

        // then - event count is 1 which is for first cube.stop()
        assertEventFired(BeforeStop.class, 1);
        assertEventFired(AfterStop.class, 1);
    }

    @Test
    public void shouldNotFireLifecycleEventsIfTryingToStopAlreadyStoppedCube() {
        // given
        createStartStopCube();

        // when
        cube.stop();

        // then  - event count is 1 which is for first cube.stop()
        assertEventFired(BeforeStop.class, 1);
        assertEventFired(AfterStop.class, 1);
    }

    @Test
    public void shouldNotFireLifecycleEventsIfTryingToStopPreRunningCube() {
        cube.changeToPreRunning();
        cube.stop();
        assertEventFired(BeforeStop.class, 0);
        assertEventFired(AfterStop.class, 0);
    }

    private void createStartStopCube() {
        cube.create();
        cube.start();
        cube.stop();
    }
}
