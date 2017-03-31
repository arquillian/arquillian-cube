package org.arquillian.cube.docker.impl.client;

import java.io.File;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.arquillian.cube.HostIpContext;
import org.arquillian.cube.docker.impl.client.config.CubeContainer;
import org.arquillian.cube.docker.impl.client.config.DockerCompositions;
import org.arquillian.cube.docker.impl.client.config.Link;
import org.arquillian.cube.docker.impl.client.config.PortBinding;
import org.arquillian.cube.docker.impl.util.AbstractCliInternetAddressResolver;
import org.arquillian.cube.docker.impl.util.Boot2Docker;
import org.arquillian.cube.docker.impl.util.DockerMachine;
import org.arquillian.cube.docker.impl.util.GitHubUtil;
import org.arquillian.cube.docker.impl.util.HomeResolverUtil;
import org.arquillian.cube.docker.impl.util.Machine;
import org.arquillian.cube.docker.impl.util.OperatingSystemFamily;
import org.arquillian.cube.docker.impl.util.OperatingSystemResolver;
import org.arquillian.cube.docker.impl.util.Top;
import org.arquillian.cube.impl.util.Strings;
import org.arquillian.cube.impl.util.SystemEnvironmentVariables;
import org.arquillian.cube.spi.CubeConfiguration;
import org.arquillian.spacelift.Spacelift;
import org.arquillian.spacelift.task.net.DownloadTool;
import org.jboss.arquillian.config.descriptor.api.ArquillianDescriptor;
import org.jboss.arquillian.core.api.Injector;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.InstanceProducer;
import org.jboss.arquillian.core.api.annotation.ApplicationScoped;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;

public class CubeDockerConfigurator {

    private static Random random = new Random();
    private static Logger log = Logger.getLogger(CubeDockerConfigurator.class.getName());

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
    private InstanceProducer<OperatingSystemFamily> operatingSystemFamilyInstanceProducer;

    public void configure(@Observes CubeConfiguration event, ArquillianDescriptor arquillianDescriptor) {
        configure(arquillianDescriptor);
    }

    private void configure(ArquillianDescriptor arquillianDescriptor) {
        operatingSystemFamilyInstanceProducer.set(new OperatingSystemResolver().currentOperatingSystem().getFamily());
        Map<String, String> config = arquillianDescriptor.extension(EXTENSION_NAME).getExtensionProperties();
        CubeDockerConfigurationResolver resolver = new CubeDockerConfigurationResolver(topInstance.get(),
                dockerMachineInstance.get(),
                boot2DockerInstance.get(),
                operatingSystemFamilyInstanceProducer.get());
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

        for (Map.Entry<String, CubeContainer> container : containers.entrySet()) {

            // If it is a dynamic definition
            final String containerId = container.getKey();
            if (containerId.endsWith("*")) {
                String templateName = containerId.substring(0, containerId.lastIndexOf('*'));

                CubeContainer cubeContainer = container.getValue();

                adaptPortBindingToParallelRun(cubeContainer);
                adaptLinksToParallelRun(uuid, cubeContainer);

                String newId = generateNewName(templateName, uuid);
                resolvedContainers.put(newId, cubeContainer);
            } else {
                resolvedContainers.put(containerId, container.getValue());
            }
        }

        dockerContainersContent.setContainers(resolvedContainers);
        return cubeConfiguration;
    }

    private void adaptLinksToParallelRun(UUID uuid, CubeContainer cubeContainer) {
        final Collection<Link> links = cubeContainer.getLinks();

        if (links == null) {
            return;
        }

        for (Link link : links) {
            if (link.getName().endsWith("*")) {
                String linkTemplate = link.getName().substring(0, link.getName().lastIndexOf('*'));
                link.setName(generateNewName(linkTemplate, uuid));

                String environmentVariable = linkTemplate.toUpperCase() + "_HOSTNAME=" + link.getName();
                if (link.isAliasSet()) {
                    link.setAlias(generateNewName(link.getAlias(), uuid));
                    environmentVariable = linkTemplate.toUpperCase() + "_HOSTNAME=" + link.getAlias();
                }

                final Collection<String> env = cubeContainer.getEnv();
                if (env != null) {
                    // to avoid duplicates
                    if (env.contains(environmentVariable)) {
                        env.remove(environmentVariable);
                    }
                } else {
                    cubeContainer.setEnv(new ArrayList<String>());
                }
                cubeContainer.getEnv().add(environmentVariable);
            }
        }
    }

    private void adaptPortBindingToParallelRun(CubeContainer cubeContainer) {
        final Collection<PortBinding> portBindings = cubeContainer.getPortBindings();
        if (portBindings == null) {
            return;
        }
        for (PortBinding portBinding : portBindings) {
            final int randomPrivatePort = generateRandomPrivatePort();
            portBinding.setBound(randomPrivatePort);
        }
    }

    private String generateNewName(String containerName, UUID uuid) {
        return containerName + "_" + uuid;
    }

    private int generateRandomPrivatePort() {
        final int randomPort = random.nextInt(16383);
        return randomPort + 49152;
    }
}
