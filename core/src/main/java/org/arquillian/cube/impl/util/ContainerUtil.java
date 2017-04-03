package org.arquillian.cube.impl.util;

import java.util.Map;
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

    /**
     * Returns the cube ID for the container. By default, this is the container
     * name, but can be overridden by the user in the container properties, e.g.
     * <code>&lt;cubeId&gt;pod-name&lt;/cubeId&gt;</code>.
     *
     * @param container
     *     the arquillian container
     *
     * @return the cube ID for the specified container
     */
    public static String getCubeIDForContainer(Container container) {
        final String cubeID;
        final Map<String, String> containerProperties = container.getContainerConfiguration().getContainerProperties();
        if (containerProperties == null) {
            // test cases may not mock entire hierarchy
            cubeID = null;
        } else {
            cubeID = containerProperties.get("cubeId");
        }
        return cubeID == null ? container.getName() : cubeID;
    }
}
