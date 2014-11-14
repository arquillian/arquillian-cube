package org.arquillian.cube.impl.util;

import org.jboss.arquillian.container.spi.Container;
import org.jboss.arquillian.container.spi.ContainerRegistry;
import org.jboss.arquillian.container.spi.client.container.DeployableContainer;

public class ContainerUtil {

    public static Container getContainerByDeployableContainer(ContainerRegistry registry, DeployableContainer<?> dc) {
        for (Container container : registry.getContainers()) {
            if (dc == container.getDeployableContainer()) {
                return container;
            }
        }
        return null;
    }
    
}
