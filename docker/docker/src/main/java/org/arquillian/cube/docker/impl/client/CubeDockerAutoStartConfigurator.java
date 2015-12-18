package org.arquillian.cube.docker.impl.client;


import java.util.ArrayList;
import java.util.List;

import org.arquillian.cube.docker.impl.client.config.CubeContainers;
import org.jboss.arquillian.config.descriptor.api.ArquillianDescriptor;
import org.jboss.arquillian.container.spi.Container;
import org.jboss.arquillian.container.spi.ContainerRegistry;
import org.jboss.arquillian.core.api.annotation.Observes;

public class CubeDockerAutoStartConfigurator {

    public void configure(@Observes CubeDockerConfiguration event, ArquillianDescriptor arquillianDescriptor, ContainerRegistry registry) {

        if (event.getAutoStartContainers() == null) {
            AutoStartParser autoStartParser = resolveNotSetAutoStart(registry, event.getDockerContainersContent());
            event.setAutoStartContainers(autoStartParser);
        }

    }


    private AutoStartParser resolveNotSetAutoStart(ContainerRegistry containerRegistry, CubeContainers containers) {
        //we want to use the automatic autoconfiguration
        List<String> containersName = toContainersName(containerRegistry.getContainers());
        return new AutomaticResolutionAutoStartParser(containersName, containers);
    }


    private static List<String> toContainersName(List<Container> containers) {
        List<String> containerNames = new ArrayList<>();

        for (Container container : containers) {
            containerNames.add(container.getName());
        }

        return containerNames;
    }
}
