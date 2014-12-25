package org.arquillian.cube.impl.client;

import org.arquillian.cube.impl.client.container.CubeContainerLifecycleController;
import org.arquillian.cube.impl.client.container.CubeRemoteCommandObserver;
import org.arquillian.cube.impl.client.container.ProtocolMetadataUpdater;
import org.arquillian.cube.impl.client.container.RemapContainerController;
import org.arquillian.cube.impl.client.container.remote.CubeAuxiliaryArchiveAppender;
import org.arquillian.cube.impl.client.enricher.CubeControllerProvider;
import org.arquillian.cube.impl.client.enricher.CubeIDResourceProvider;
import org.arquillian.cube.impl.client.enricher.CubeResourceProvider;
import org.arquillian.cube.impl.containerless.ContainerlessDockerDeployableContainer;
import org.jboss.arquillian.container.spi.client.container.DeployableContainer;
import org.jboss.arquillian.container.test.spi.client.deployment.AuxiliaryArchiveAppender;
import org.jboss.arquillian.core.spi.LoadableExtension;
import org.jboss.arquillian.test.spi.enricher.resource.ResourceProvider;

public class CubeExtension implements LoadableExtension {

    @Override
    public void register(ExtensionBuilder builder) {
        builder.observer(CubeConfigurator.class)
               .observer(CubeClientCreator.class)
               .observer(CubeRegistrar.class)
               .observer(CubeLifecycleController.class)
               .observer(CubeSuiteLifecycleController.class)
               .observer(ClientCubeControllerCreator.class);

        builder.service(ResourceProvider.class, CubeResourceProvider.class);
        builder.service(ResourceProvider.class, CubeControllerProvider.class);

        // Arquillian Container integration
        builder.observer(ProtocolMetadataUpdater.class)
               .observer(CubeContainerLifecycleController.class)
               .observer(RemapContainerController.class)
               .observer(CubeRemoteCommandObserver.class);

        builder.service(AuxiliaryArchiveAppender.class, CubeAuxiliaryArchiveAppender.class);
        builder.service(ResourceProvider.class, CubeIDResourceProvider.class);
        builder.service(DeployableContainer.class, ContainerlessDockerDeployableContainer.class);
    }

}
