package org.arquillian.cube.docker.impl.model;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.mockito.Matchers.anyString;

import com.github.dockerjava.api.exception.NotFoundException;
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

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectContainerCmd;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Ports;

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
        cube = injectorInst.get().inject(new DockerCube(ID, new CubeContainer(), executor));
    }

    @Test
    public void shouldFireLifecycleEventsDuringCreate() {
        cube.create();
        assertEventFired(BeforeCreate.class, 1);
        assertEventFired(AfterCreate.class, 1);
    }

    @Test
    public void shouldFireLifecycleEventsDuringStart() {
        cube.start();
        assertEventFired(BeforeStart.class, 1);
        assertEventFired(AfterStart.class, 1);
    }

    @Test
    public void shouldFireLifecycleEventsDuringStop() {
        cube.stop();
        assertEventFired(BeforeStop.class, 1);
        assertEventFired(AfterStop.class, 1);
    }

    @Test
    public void shouldFireLifecycleEventsDuringStopWhenContainerNotFound() {
        doThrow(new NotFoundException("container not found"))
            .when(executor).stopContainer(ID);
        cube.stop();
        assertEventFired(BeforeStop.class, 1);
        assertEventFired(AfterStop.class, 1);
    }

    @Test
    public void shouldFireLifecycleEventsDuringDestroy() {
        cube.stop(); // require a stopped Cube to destroy it.
        cube.destroy();
        assertEventFired(BeforeDestroy.class, 1);
        assertEventFired(AfterDestroy.class, 1);
    }

    @Test
    public void shouldFireLifecycleEventsDuringDestroyWhenContainerNotFound() {
        doThrow(new NotFoundException("container not found"))
            .when(executor).removeContainer(ID);
        cube.stop();
        cube.destroy();
        assertEventFired(BeforeDestroy.class, 1);
        assertEventFired(AfterDestroy.class, 1);
    }
}
