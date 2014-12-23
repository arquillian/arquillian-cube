package org.arquillian.cube.impl.client.container;

import java.util.List;

import org.arquillian.cube.impl.client.CubeConfiguration;
import org.arquillian.cube.impl.docker.DockerClientExecutor;
import org.arquillian.cube.impl.util.ContainerUtil;
import org.arquillian.cube.spi.Cube;
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
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;

public class CubeContainerLifecycleController {

    @Inject
    private Event<CubeControlEvent> controlEvent;

    @Inject
    private Instance<DockerClientExecutor> dockerClientExecutor;
    
    public void startCubeMappedContainer(@Observes BeforeStart event, CubeRegistry cubeRegistry,
            ContainerRegistry containerRegistry, CubeConfiguration cubeConfiguration) {
        Container container = ContainerUtil.getContainerByDeployableContainer(containerRegistry,
                event.getDeployableContainer());
        if (container == null) {
            return;
        }

        Cube cube = cubeRegistry.getCube(container.getName());
        if (cube == null) {
            return; // No Cube found matching Container name, not managed by Cube
        }

        if(cubeConfiguration.shouldAllowToConnectToRunningContainers() && isCubeRunning(cube)) {
            controlEvent.fire(new PreRunningCube(cube));
            return; //Container is already running and user has configured to reuse it.
        }
        
        controlEvent.fire(new CreateCube(cube));
        controlEvent.fire(new StartCube(cube));
    }

    public void stopCubeMappedContainer(@Observes AfterStop event, CubeRegistry cubeRegistry,
            ContainerRegistry containerRegistry) {
        Container container = ContainerUtil.getContainerByDeployableContainer(containerRegistry,
                event.getDeployableContainer());
        if (container == null) {
            return;
        }

        Cube cube = cubeRegistry.getCube(container.getName());
        if (cube == null) {
            return; // No Cube found matching Container name, not managed by Cube
        }

        controlEvent.fire(new StopCube(cube));
        controlEvent.fire(new DestroyCube(cube));
    }
    
    private boolean isCubeRunning(Cube cube) {
      //TODO should we create an adapter class so we don't expose client classes in this part?
        List<com.github.dockerjava.api.model.Container> runningContainers = dockerClientExecutor.get().listRunningContainers();
        for (com.github.dockerjava.api.model.Container container : runningContainers) {
            for (String name : container.getNames()) {
                if(name.startsWith("/")) name = name.substring(1); //Names array adds an slash to the docker name container.
                if(name.equals(cube.getId())) { //cube id is the container name in docker0 Id in docker is the hash that identifies it.
                    return true;
                }
            }
        }
        
        return false;
    }
}
