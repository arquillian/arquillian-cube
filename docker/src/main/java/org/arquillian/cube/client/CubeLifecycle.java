package org.arquillian.cube.client;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.arquillian.cube.await.AwaitStrategyFactory;
import org.arquillian.cube.await.PollingAwaitStrategy;
import org.arquillian.cube.docker.DockerClientExecutor;
import org.jboss.arquillian.container.spi.ContainerRegistry;
import org.jboss.arquillian.container.spi.event.container.AfterStop;
import org.jboss.arquillian.container.spi.event.container.BeforeSetup;
import org.jboss.arquillian.container.spi.event.container.BeforeStart;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.test.spi.event.suite.AfterSuite;
import org.jboss.arquillian.test.spi.event.suite.BeforeSuite;

import com.github.dockerjava.api.command.CreateContainerResponse;

public class CubeLifecycle {

    @Inject
    private Instance<CubeConfiguration> cubeConfigurationInstance;

    @Inject
    private Instance<ContainerRegistry> reg;

    private DockerClientExecutor dockerClientExecutor;
    private Set<CreateContainerResponse> createdContainers = new HashSet<>();

    public void startDockerImage(@Observes(precedence = 0) BeforeSuite event) {
        // starts suite containers
    }

    public void stopDockerImage(@Observes AfterSuite event) {
        // stops suite containers
    }

    public void overrideContainerHostProperty(@Observes BeforeSetup event) {
        // TODO need to override host property to boot2docker ip if host is set to boot2docker value.
    }

    public void startDockerImage(@Observes BeforeStart event) {
        // start container managed by Arquillian

        CubeConfiguration cubeConfiguration = cubeConfigurationInstance.get();

        dockerClientExecutor = new DockerClientExecutor(cubeConfiguration);

        Map<String, Object> dockerContainersContent = cubeConfiguration.getDockerContainersContent();

        Set<String> containerNames = dockerContainersContent.keySet();

        for (String containerName : containerNames) {
            Map<String, Object> containerConfiguration = (Map<String, Object>) dockerContainersContent
                    .get(containerName);
            CreateContainerResponse createContainer = dockerClientExecutor.createContainer(containerName,
                    containerConfiguration);
            dockerClientExecutor.startContainer(createContainer, containerConfiguration);

            if(!AwaitStrategyFactory.create(dockerClientExecutor, createContainer, containerConfiguration).await()) {
                throw new IllegalArgumentException(String.format("Cannot connect to %s container", containerName));
            }

            createdContainers.add(createContainer);
        }

    }

    public void stopDockerImage(@Observes AfterStop event) {
        // stops container managed by Arquillian

        for (CreateContainerResponse createContainerResponse : createdContainers) {
            dockerClientExecutor.stopContainer(createContainerResponse);
            dockerClientExecutor.removeContainer(createContainerResponse);
        }
    }

}
