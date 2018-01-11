package org.arquillian.cube.docker.impl.client;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.logging.Logger;
import org.arquillian.cube.HostIpContext;
import org.arquillian.cube.docker.impl.client.config.CubeContainer;
import org.arquillian.cube.docker.impl.client.config.DockerCompositions;
import org.arquillian.cube.docker.impl.client.config.StarOperator;
import org.arquillian.cube.docker.impl.util.Boot2Docker;
import org.arquillian.cube.docker.impl.util.DockerMachine;
import org.arquillian.cube.docker.impl.util.OperatingSystemFamily;
import org.arquillian.cube.docker.impl.util.OperatingSystemResolver;
import org.arquillian.cube.docker.impl.util.Top;
import org.arquillian.cube.spi.CubeConfiguration;
import org.jboss.arquillian.config.descriptor.api.ArquillianDescriptor;
import org.jboss.arquillian.container.spi.Container;
import org.jboss.arquillian.container.spi.ContainerRegistry;
import org.jboss.arquillian.core.api.Injector;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.InstanceProducer;
import org.jboss.arquillian.core.api.annotation.ApplicationScoped;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;

public class CubeDockerConfigurator {

    public static final String DOCKER_HOST = "DOCKER_HOST";
    private static final String EXTENSION_NAME = "docker";
    private static Random random = new Random();
    private static Logger log = Logger.getLogger(CubeDockerConfigurator.class.getName());
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
    private InstanceProducer<OperatingSystemFamily> operatingSystemFamilyInstanceProducer;

    @Inject
    @ApplicationScoped
    private Instance<ContainerRegistry> registry;

    public CubeDockerConfigurator() {
    }

    public CubeDockerConfigurator(
        Instance<ContainerRegistry> registry) {
        this.registry = registry;
    }

    public void configure(@Observes CubeConfiguration event, ArquillianDescriptor arquillianDescriptor) {
        configure(arquillianDescriptor);
    }

    private void configure(ArquillianDescriptor arquillianDescriptor) {
        operatingSystemFamilyInstanceProducer.set(new OperatingSystemResolver().currentOperatingSystem().getFamily());
        Map<String, String> config = arquillianDescriptor.extension(EXTENSION_NAME).getExtensionProperties();
        CubeDockerConfigurationResolver resolver =
            new CubeDockerConfigurationResolver(topInstance.get(), dockerMachineInstance.get(),
                boot2DockerInstance.get(), operatingSystemFamilyInstanceProducer.get());
        resolver.resolve(config);
        CubeDockerConfiguration cubeConfiguration = CubeDockerConfiguration.fromMap(config, injectorInstance.get());
        cubeConfiguration = resolveDynamicNames(cubeConfiguration);
        System.out.println(cubeConfiguration);
        hostUriContextInstanceProducer.set(new HostIpContext(cubeConfiguration.getDockerServerIp()));
        configurationProducer.set(cubeConfiguration);
    }

    CubeDockerConfiguration resolveDynamicNames(CubeDockerConfiguration cubeConfiguration) {

        final Map<String, CubeContainer> resolvedContainers = new HashMap<>();

        final DockerCompositions dockerContainersContent = cubeConfiguration.getDockerContainersContent();
        final Map<String, CubeContainer> containers = dockerContainersContent.getContainers();

        final UUID uuid = UUID.randomUUID();

        for (Map.Entry<String, CubeContainer> cubeContainerEntry : containers.entrySet()) {

            // If it is a dynamic definition
            final String containerId = cubeContainerEntry.getKey();
            if (containerId.endsWith("*")) {
                String templateName = containerId.substring(0, containerId.lastIndexOf('*'));

                CubeContainer cubeContainer = cubeContainerEntry.getValue();

                StarOperator.adaptPortBindingToParallelRun(cubeContainer);
                StarOperator.adaptLinksToParallelRun(uuid, cubeContainer);
                StarOperator.adaptDependenciesToParallelRun(uuid, cubeContainer);

                String newId = StarOperator.generateNewName(templateName, uuid);
                // Due to the fact that arquillian container names are determined way before the cubeDockerConfigurator kicks of and
                // changes the name, we need to update the name
                for (Container container : registry.get().getContainers()) {
                    if (container.getName().equals(templateName)) {
                        try {
                            Field name = container.getClass().getDeclaredField("name");
                            name.setAccessible(true);
                            name.set(container, newId);
                        } catch (NoSuchFieldException | IllegalAccessException e) {
                            log.warning("Could not replace name");
                        }
                    }
                }

                resolvedContainers.put(newId, cubeContainer);
            } else {
                resolvedContainers.put(containerId, cubeContainerEntry.getValue());
            }
        }

        dockerContainersContent.setContainers(resolvedContainers);
        return cubeConfiguration;
    }
}
