package org.arquillian.cube.impl.client.container;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.arquillian.cube.impl.util.BindingUtil;
import org.arquillian.cube.spi.Binding;
import org.arquillian.cube.spi.Binding.PortBinding;
import org.arquillian.cube.spi.Cube;
import org.arquillian.cube.spi.CubeRegistry;
import org.jboss.arquillian.config.descriptor.api.ContainerDef;
import org.jboss.arquillian.container.spi.Container;
import org.jboss.arquillian.container.spi.ContainerRegistry;
import org.jboss.arquillian.container.spi.client.container.DeployableContainer;
import org.jboss.arquillian.container.spi.event.container.BeforeSetup;
import org.jboss.arquillian.core.api.annotation.Observes;

public class RemapContainerController {

    public void remapContainer(@Observes BeforeSetup event, CubeRegistry cubeRegistry,
            ContainerRegistry containerRegistry) {

        Container container = getContainerByDeployableContainer(containerRegistry, event.getDeployableContainer());
        if (container == null) {
            return;
        }

        Cube cube = cubeRegistry.getCube(container.getName());
        if (cube == null) {
            return; // No Cube found matching Container name, not managed by Cube
        }

        ContainerDef containerConfiguration = container.getContainerConfiguration();

        Map<String, String> containerProperties = containerConfiguration.getContainerProperties();
        
        Binding binding = BindingUtil.binding(cube.configuration());
        
        Set<Entry<String, String>> properties = containerProperties.entrySet();
        for (Entry<String, String> property : properties) {
            if (property.getKey().matches("(?i:.*port.*)")) {
                int containerPort = Integer.parseInt(property.getValue());

                PortBinding bindingForExposedPort = null;
                if ((bindingForExposedPort = binding.getBindingForExposedPort(containerPort)) != null) {
                    containerConfiguration.overrideProperty(property.getKey(),
                            Integer.toString(bindingForExposedPort.getBindingPort()));
                }

            }
        }
    }

    private Container getContainerByDeployableContainer(ContainerRegistry registry, DeployableContainer<?> dc) {
        for (Container container : registry.getContainers()) {
            if (dc == container.getDeployableContainer()) {
                return container;
            }
        }
        return null;
    }

}
