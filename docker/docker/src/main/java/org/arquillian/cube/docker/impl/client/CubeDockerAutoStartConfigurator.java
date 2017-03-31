package org.arquillian.cube.docker.impl.client;

import java.util.ArrayList;
import java.util.List;
import org.arquillian.cube.docker.impl.client.config.DockerCompositions;
import org.arquillian.cube.impl.util.ContainerUtil;
import org.arquillian.cube.spi.AutoStartParser;
import org.jboss.arquillian.config.descriptor.api.ArquillianDescriptor;
import org.jboss.arquillian.container.spi.Container;
import org.jboss.arquillian.container.spi.ContainerRegistry;
import org.jboss.arquillian.core.api.annotation.Observes;

public class CubeDockerAutoStartConfigurator {

    private static List<String> toContainersName(List<Container> containers) {
        List<String> containerNames = new ArrayList<>();

        for (Container container : containers) {
            containerNames.add(ContainerUtil.getCubeIDForContainer(container));
        }

        return containerNames;
    }

    public void configure(@Observes CubeDockerConfiguration event, ArquillianDescriptor arquillianDescriptor,
        ContainerRegistry registry) {

        if (event.getAutoStartContainers() == null) {
            AutoStartParser autoStartParser = resolveNotSetAutoStart(registry, event.getDockerContainersContent());
            event.setAutoStartContainers(autoStartParser);
        }
    }

    private AutoStartParser resolveNotSetAutoStart(ContainerRegistry containerRegistry, DockerCompositions containers) {
        //we want to use the automatic autoconfiguration
        List<String> containersName = toContainersName(containerRegistry.getContainers());

        if (containers.getNetworkIds().size() > 0) {
            //if network defined then you should not mix links and network
            return new AutomaticResolutionNetworkAutoStartParser(containersName, containers);
        } else {
            // if no network defined then links approach is used.
            return new AutomaticResolutionLinksAutoStartParser(containersName, containers);
        }
    }
}
