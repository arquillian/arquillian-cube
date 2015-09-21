package org.arquillian.cube.docker.impl.client;


import org.jboss.arquillian.config.descriptor.api.ArquillianDescriptor;
import org.jboss.arquillian.container.spi.Container;
import org.jboss.arquillian.container.spi.ContainerRegistry;
import org.jboss.arquillian.core.api.annotation.Observes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CubeDockerAutoStartConfigurator {

    public void configure(@Observes CubeDockerConfiguration event, ArquillianDescriptor arquillianDescriptor, ContainerRegistry registry) {

        if (event.getAutoStartContainers() == null) {
            AutoStartParser autoStartParser = resolveNotSetAutoStart(registry, event.getDockerContainersContent());
            event.setAutoStartContainers(autoStartParser);
        }

    }


    private AutoStartParser resolveNotSetAutoStart(ContainerRegistry containerRegistry, Map<String, Object> containerDefinition) {
        //we want to use the automatic autoconfiguration
        List<String> containersName = toContainersName(containerRegistry.getContainers());
        return new AutomaticResolutionAutoStartParser(containersName, containerDefinition);
    }


    private static List<String> toContainersName(List<Container> containers) {
        List<String> containerNames = new ArrayList<>();

        for (Container container : containers) {
            containerNames.add(container.getName());
        }

        return containerNames;
    }
}
