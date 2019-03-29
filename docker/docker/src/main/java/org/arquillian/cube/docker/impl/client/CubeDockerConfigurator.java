package org.arquillian.cube.docker.impl.client;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.arquillian.cube.HostIpContext;
import org.arquillian.cube.docker.impl.client.config.CubeContainer;
import org.arquillian.cube.docker.impl.client.config.DockerCompositions;
import org.arquillian.cube.docker.impl.client.config.Network;
import org.arquillian.cube.docker.impl.client.config.StarOperator;
import org.arquillian.cube.docker.impl.util.Boot2Docker;
import org.arquillian.cube.docker.impl.util.DefaultDocker;
import org.arquillian.cube.docker.impl.util.DockerMachine;
import org.arquillian.cube.docker.impl.util.OperatingSystemInterface;
import org.arquillian.cube.docker.impl.util.OperatingSystemResolver;
import org.arquillian.cube.docker.impl.util.Top;
import org.arquillian.cube.impl.model.ContainerMetadata;
import org.arquillian.cube.spi.CubeConfiguration;
import org.jboss.arquillian.config.descriptor.api.ArquillianDescriptor;
import org.jboss.arquillian.core.api.Injector;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.InstanceProducer;
import org.jboss.arquillian.core.api.annotation.ApplicationScoped;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;

public class CubeDockerConfigurator {

    public static final String DOCKER_HOST = "DOCKER_HOST";
    private static final String EXTENSION_NAME = "docker";

    @Inject
    @ApplicationScoped
    private InstanceProducer<CubeDockerConfiguration> configurationProducer;

    @Inject
    @ApplicationScoped
    private InstanceProducer<HostIpContext> hostUriContextInstanceProducer;

    @Inject
    private Instance<Boot2Docker> boot2DockerInstance;

    @Inject
    private Instance<DockerMachine> dockerMachineInstance;

    @Inject
    private Instance<Top> topInstance;

    @Inject
    private Instance<Injector> injectorInstance;

    @Inject
    @ApplicationScoped
    private InstanceProducer<OperatingSystemInterface> operatingSystemInstanceProducer;

    @Inject
    @ApplicationScoped
    private InstanceProducer<ContainerMetadata> containerMetadataInstanceProducer;

    /**
     *
     * @param event CubeConfiguration
     * @param arquillianDescriptor ArquillianDescriptor
     */
    public void configure(@Observes CubeConfiguration event, ArquillianDescriptor arquillianDescriptor) {
        configure(arquillianDescriptor);
    }

    OperatingSystemInterface getCurrentOperatingSystem() {
        return new OperatingSystemResolver().currentOperatingSystem();
    }

    private void configure(ArquillianDescriptor arquillianDescriptor) {

        if (!Optional.ofNullable(operatingSystemInstanceProducer.get()).isPresent()) {
            operatingSystemInstanceProducer.set(getCurrentOperatingSystem());
        }

        Map<String, String> config = arquillianDescriptor.extension(EXTENSION_NAME).getExtensionProperties();
        CubeDockerConfigurationResolver resolver = new CubeDockerConfigurationResolver(topInstance.get(),
            dockerMachineInstance.get(), boot2DockerInstance.get(), new DefaultDocker(),
            operatingSystemInstanceProducer.get());
        resolver.resolve(config);
        CubeDockerConfiguration cubeConfiguration = CubeDockerConfiguration.fromMap(config, injectorInstance.get());
        cubeConfiguration = resolveDynamicNames(cubeConfiguration);
        System.out.println(cubeConfiguration);
        hostUriContextInstanceProducer.set(new HostIpContext(cubeConfiguration.getDockerServerIp()));
        configurationProducer.set(cubeConfiguration);
    }

    CubeDockerConfiguration resolveDynamicNames(CubeDockerConfiguration cubeConfiguration) {
        final UUID uuid = UUID.randomUUID();

        final DockerCompositions dockerContainersContent = cubeConfiguration.getDockerContainersContent();
        final Map<String, Network> networks = dockerContainersContent.getNetworks();
        final Map<String, String> networkResolutions = new HashMap<>();
        if (networks != null) {
            final Map<String, Network> resolvedNetworks = new HashMap<>();
            for (Map.Entry<String, Network> network : networks.entrySet()) {
                final String networkId = network.getKey();
                if (networkId.endsWith("*")) {
                    String templateName = networkId.substring(0, networkId.lastIndexOf("*"));
                    String newId = StarOperator.generateNewName(templateName, uuid);
                    resolvedNetworks.put(newId, network.getValue());
                    networkResolutions.put(networkId, newId);
                } else {
                    resolvedNetworks.put(networkId, network.getValue());
                }
            }
            dockerContainersContent.setNetworks(resolvedNetworks);
        }
        final Map<String, CubeContainer> resolvedContainers = new HashMap<>();
        final Map<String, String> containerMetadata = new HashMap<>();
        final Map<String, CubeContainer> containers = dockerContainersContent.getContainers();
        for (Map.Entry<String, CubeContainer> container : containers.entrySet()) {

            // If it is a dynamic definition
            final String containerId = container.getKey();
            CubeContainer cubeContainer = container.getValue();

            StarOperator.adaptNetworksToParalledRun(networkResolutions, cubeContainer);
            if (containerId.endsWith("*")) {
                String templateName = containerId.substring(0, containerId.lastIndexOf('*'));

                StarOperator.adaptPortBindingToParallelRun(cubeContainer);
                StarOperator.adaptLinksToParallelRun(uuid, cubeContainer);
                StarOperator.adaptDependenciesToParallelRun(uuid, cubeContainer);

                String newId = StarOperator.generateNewName(templateName, uuid);
                resolvedContainers.put(newId, cubeContainer);
                containerMetadata.put(templateName, newId);
            } else {
                resolvedContainers.put(containerId, cubeContainer);
            }
        }
        if (containerMetadataInstanceProducer != null && !containerMetadata.isEmpty()) {
            containerMetadataInstanceProducer.set(new ContainerMetadata(containerMetadata));
        }
        dockerContainersContent.setContainers(resolvedContainers);
        return cubeConfiguration;
    }

}
