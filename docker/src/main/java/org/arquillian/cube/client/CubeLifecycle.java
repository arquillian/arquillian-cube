package org.arquillian.cube.client;

import java.util.Map;

import org.arquillian.cube.await.AwaitStrategyFactory;
import org.arquillian.cube.docker.DockerClientExecutor;
import org.jboss.arquillian.container.spi.Container;
import org.jboss.arquillian.container.spi.ContainerRegistry;
import org.jboss.arquillian.container.spi.client.container.DeployableContainer;
import org.jboss.arquillian.container.spi.event.container.AfterStop;
import org.jboss.arquillian.container.spi.event.container.BeforeSetup;
import org.jboss.arquillian.container.spi.event.container.BeforeStart;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.InstanceProducer;
import org.jboss.arquillian.core.api.annotation.ApplicationScoped;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.test.spi.event.suite.AfterSuite;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;

public class CubeLifecycle {

    @Inject
    @ApplicationScoped
    private InstanceProducer<ContainerMapping> containerMappingInstance;
    
    @Inject
    @ApplicationScoped
    private InstanceProducer<DockerClientExecutor> dockerClientExecutorInstance;
    
    @Inject
    private Instance<CubeConfiguration> cubeConfigurationInstance;

    @Inject
    private Instance<ContainerRegistry> reg;

    private DockerClientExecutor dockerClientExecutor;

    public void startDockerImage(@Observes CubeConfiguration cubeConfiguration) {
        // starts suite containers

        this.dockerClientExecutor = new DockerClientExecutor(cubeConfiguration);
        this.dockerClientExecutorInstance.set(this.dockerClientExecutor);

        ContainerMapping containerMapping = new ContainerMapping();
        this.containerMappingInstance.set(containerMapping);
    }

    public void stopDockerImage(@Observes AfterSuite event) {
        // stops suite containers
    }

    public void overrideContainerHostProperty(@Observes BeforeSetup event) {
        // TODO need to override host property to boot2docker ip if host is set to boot2docker value.
    }

    public void startDockerImage(@Observes BeforeStart event, CubeConfiguration cubeConfiguration, ContainerMapping containerMapping) {
        // start container managed by Arquillian

        Map<String, Object> dockerContainersContent = cubeConfiguration.getDockerContainersContent();

        Container container = getContainerByDeployableContainer(event.getDeployableContainer());
        String containerName = container.getName();
        @SuppressWarnings("unchecked")
        Map<String, Object> containerConfiguration = (Map<String, Object>) dockerContainersContent.get(containerName);
        if(containerConfiguration == null) {
            return; // no docker mapping found for this container
        }
        CreateContainerResponse createContainer = this.dockerClientExecutor.createContainer(containerName, containerConfiguration);
        dockerClientExecutor.startContainer(createContainer, containerConfiguration);

        if(!AwaitStrategyFactory.create(this.dockerClientExecutor, createContainer, containerConfiguration).await()) {
            throw new IllegalArgumentException(String.format("Cannot connect to %s container", containerName));
        }
        containerMapping.addContainer(containerName, createContainer.getId());
    }

    public void stopDockerImage(@Observes AfterStop event, ContainerMapping containerMapping) {
        // stops container managed by Arquillian

        Container container = getContainerByDeployableContainer(event.getDeployableContainer());
        String id = containerMapping.removeContainer(container.getName());
        if(id != null) { // no docker image started for this container
            DockerClient client = this.dockerClientExecutor.getDockerClient();
            client.stopContainerCmd(id).exec();
            client.removeContainerCmd(id).exec();
        }
    }

    private Container getContainerByDeployableContainer(DeployableContainer<?> dc) {
        ContainerRegistry registry = reg.get();
        for(Container container : registry.getContainers()) {
            if(dc == container.getDeployableContainer()) {
                return container;
            }
        }
        return null;
    }
}
