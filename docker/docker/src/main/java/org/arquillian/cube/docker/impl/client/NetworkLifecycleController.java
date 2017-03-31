package org.arquillian.cube.docker.impl.client;

import java.util.Map;
import java.util.Set;
import org.arquillian.cube.docker.impl.client.config.CubeContainer;
import org.arquillian.cube.docker.impl.client.config.DockerCompositions;
import org.arquillian.cube.docker.impl.client.config.Network;
import org.arquillian.cube.docker.impl.docker.DockerClientExecutor;
import org.arquillian.cube.docker.impl.model.NetworkRegistry;
import org.arquillian.cube.spi.CubeConfiguration;
import org.arquillian.cube.spi.event.lifecycle.AfterCreate;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.test.spi.event.suite.AfterSuite;
import org.jboss.arquillian.test.spi.event.suite.BeforeSuite;

public class NetworkLifecycleController {

    @Inject
    private Instance<NetworkRegistry> networkRegistryInstance;

    @Inject
    private Instance<DockerClientExecutor> dockerClientExecutorInstance;

    public void createNetworks(@Observes(precedence = 200) BeforeSuite event, CubeConfiguration cubeConfiguration,
        CubeDockerConfiguration dockerConfiguration) {
        final DockerCompositions dockerContainersContent = dockerConfiguration.getDockerContainersContent();
        final Map<String, Network> networks = dockerContainersContent.getNetworks();
        final NetworkRegistry networkRegistry = networkRegistryInstance.get();
        final DockerClientExecutor dockerClientExecutor = dockerClientExecutorInstance.get();

        for (Map.Entry<String, Network> network : networks.entrySet()) {
            final String id = dockerClientExecutor.createNetwork(network.getKey(), network.getValue());
            networkRegistry.addNetwork(id, network.getValue());
        }
    }

    public void destroyNetworks(@Observes(precedence = -200) AfterSuite event, CubeDockerConfiguration configuration) {
        final NetworkRegistry networkRegistry = networkRegistryInstance.get();
        final DockerClientExecutor dockerClientExecutor = dockerClientExecutorInstance.get();

        final Set<String> networkIds = networkRegistry.getNetworkIds();

        for (String networkId : networkIds) {
            dockerClientExecutor.removeNetwork(networkId);
        }
    }

    public void connectToNetworks(@Observes AfterCreate event, CubeDockerConfiguration dockerConfiguration) {
        final DockerCompositions dockerContainersContent = dockerConfiguration.getDockerContainersContent();
        final DockerClientExecutor dockerClientExecutor = dockerClientExecutorInstance.get();
        String cubeId = event.getCubeId();
        CubeContainer container = dockerContainersContent.get(cubeId);
        if (container != null && container.getNetworks() != null) {
            for (String network : container.getNetworks()) {
                if (!network.equals(container.getNetworkMode())) {
                    dockerClientExecutor.connectToNetwork(network, cubeId);
                }
            }
        }
    }
}
