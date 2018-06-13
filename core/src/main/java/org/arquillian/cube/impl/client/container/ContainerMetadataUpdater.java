package org.arquillian.cube.impl.client.container;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.logging.Logger;
import org.arquillian.cube.impl.model.ContainerMetadata;
import org.jboss.arquillian.container.spi.Container;
import org.jboss.arquillian.container.spi.ContainerRegistry;
import org.jboss.arquillian.core.api.annotation.Observes;

public class ContainerMetadataUpdater {

    private static final Logger log = Logger.getLogger(ContainerMetadataUpdater.class.getName());

    public void updateResolvedContainersName(@Observes ContainerMetadata containerMetadata,
        ContainerRegistry containerRegistry) {
        final Map<String, String> containers = containerMetadata.getNames();
        for (Container container : containerRegistry.getContainers()) {
            if (containers.containsKey(container.getName())) {
                final String mappedContainerName = containers.get(container.getName());
                try {
                    Field name = container.getClass().getDeclaredField("name");
                    name.setAccessible(true);
                    name.set(container, mappedContainerName);
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    log.warning(
                        String.format("Failed to update name %s for container %s", mappedContainerName,
                            container.getName()));
                }
            }
        }
    }
}
