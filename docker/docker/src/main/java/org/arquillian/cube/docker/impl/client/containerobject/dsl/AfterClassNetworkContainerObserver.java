package org.arquillian.cube.docker.impl.client.containerobject.dsl;

import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.arquillian.cube.docker.impl.docker.DockerClientExecutor;
import org.arquillian.cube.docker.impl.model.NetworkRegistry;
import org.arquillian.cube.spi.metadata.IsNetworkContainerObject;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.test.spi.event.suite.AfterClass;

/**
 * Observer that removes network objects after test execution.
 */
public class AfterClassNetworkContainerObserver {

    private static Logger logger = Logger.getLogger(AfterClassNetworkContainerObserver.class.getName());

    @Inject
    Instance<DockerClientExecutor> dockerClientExecutorInstance;

    @Inject
    private Instance<NetworkRegistry> networkRegistryInstance;

    //To be executed after container objects are stopped (AfterClassContainerObjectObserver)
    public void stopNetworkObjects(@Observes(precedence = 90) AfterClass afterClass) {

        final NetworkRegistry networkRegistry = networkRegistryInstance.get();
        final List<String> networksToRemove = networkRegistry.getNetworkIds()
            .stream()
            .filter(id -> networkRegistry.getNetwork(id).hasMetadata(IsNetworkContainerObject.class))
            .collect(Collectors.toList());

        // To avoid Concurrent modification exception
        networksToRemove.stream().forEach(id -> {
            logger.fine(String.format("Stopping Network %s", id));
            dockerClientExecutorInstance.get().removeNetwork(id);
            networkRegistry.removeNetwork(id);
        });
    }
}
