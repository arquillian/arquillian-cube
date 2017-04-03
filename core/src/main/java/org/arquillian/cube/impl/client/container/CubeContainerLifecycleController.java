package org.arquillian.cube.impl.client.container;

import org.arquillian.cube.impl.util.ContainerUtil;
import org.arquillian.cube.spi.ConnectionMode;
import org.arquillian.cube.spi.Cube;
import org.arquillian.cube.spi.CubeConfiguration;
import org.arquillian.cube.spi.CubeRegistry;
import org.arquillian.cube.spi.event.CreateCube;
import org.arquillian.cube.spi.event.CubeControlEvent;
import org.arquillian.cube.spi.event.DestroyCube;
import org.arquillian.cube.spi.event.PreRunningCube;
import org.arquillian.cube.spi.event.StartCube;
import org.arquillian.cube.spi.event.StopCube;
import org.jboss.arquillian.container.spi.Container;
import org.jboss.arquillian.container.spi.ContainerRegistry;
import org.jboss.arquillian.container.spi.event.container.AfterStop;
import org.jboss.arquillian.container.spi.event.container.BeforeStart;
import org.jboss.arquillian.core.api.Event;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;

public class CubeContainerLifecycleController {

    @Inject
    private Event<CubeControlEvent> controlEvent;

    public void startCubeMappedContainer(@Observes BeforeStart event, CubeRegistry cubeRegistry,
        ContainerRegistry containerRegistry, CubeConfiguration cubeConfiguration) {
        Container container = ContainerUtil.getContainerByDeployableContainer(containerRegistry,
            event.getDeployableContainer());
        if (container == null) {
            return;
        }

        Cube<?> cube = cubeRegistry.getCube(ContainerUtil.getCubeIDForContainer(container));
        if (cube == null) {
            return; // No Cube found matching Container name, not managed by Cube
        }
        ConnectionMode connectionMode = cubeConfiguration.getConnectionMode();

        if (connectionMode.isAllowReconnect() && cube.isRunningOnRemote()) {
            controlEvent.fire(new PreRunningCube(cube));
            return;
        }

        controlEvent.fire(new CreateCube(cube));
        controlEvent.fire(new StartCube(cube));

        if (connectionMode.isAllowReconnect() && !connectionMode.isStoppable()) {
            // If we allow reconnections and containers are none stoppable which means that they will be able to be
            // reused in next executions then at this point we can assume that the container is a prerunning container.

            controlEvent.fire(new PreRunningCube(cube));
        }
    }

    public void stopCubeMappedContainer(@Observes AfterStop event, CubeRegistry cubeRegistry,
        ContainerRegistry containerRegistry) {
        Container container = ContainerUtil.getContainerByDeployableContainer(containerRegistry,
            event.getDeployableContainer());
        if (container == null) {
            return;
        }

        Cube<?> cube = cubeRegistry.getCube(ContainerUtil.getCubeIDForContainer(container));
        if (cube == null) {
            return; // No Cube found matching Container name, not managed by Cube
        }

        controlEvent.fire(new StopCube(cube));
        controlEvent.fire(new DestroyCube(cube));
    }
}
