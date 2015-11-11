package org.arquillian.cube.docker.impl.client;

import org.arquillian.cube.docker.impl.client.container.DockerServerIPConfigurator;
import org.arquillian.cube.docker.impl.client.enricher.CubeResourceProvider;
import org.jboss.arquillian.core.spi.LoadableExtension;
import org.jboss.arquillian.test.spi.enricher.resource.ResourceProvider;

public class CubeDockerExtension implements LoadableExtension {

    @Override
    public void register(ExtensionBuilder builder) {
        builder.observer(CubeDockerConfigurator.class)
               .observer(DockerClientCreator.class)
               .observer(CubeDockerRegistrar.class)
               .observer(CubeSuiteLifecycleController.class)
               //.observer(ClientCubeControllerCreator.class)
               .observer(BeforeStopContainerObserver.class)
               .observer(Boot2DockerCreator.class);

        builder.service(ResourceProvider.class, CubeResourceProvider.class);

        // Arquillian Container integration
        // Only register if container-test-spi is on classpath
        if(Validate.classExists("org.jboss.arquillian.container.test.spi.client.deployment.AuxiliaryArchiveAppender")) {
            builder.observer(DockerServerIPConfigurator.class);
        }
    }

}
