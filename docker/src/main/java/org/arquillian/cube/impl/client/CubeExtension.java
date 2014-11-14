package org.arquillian.cube.impl.client;

import org.arquillian.cube.impl.client.container.CubeContainerLifecycleController;
import org.arquillian.cube.impl.client.container.ProtocolMetadataUpdater;
import org.arquillian.cube.impl.client.enricher.CubeIDResourceProvider;
import org.arquillian.cube.impl.client.enricher.CubeResourceProvider;
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
               .observer(CubeContainerLifecycleController.class);

        builder.observer(ProtocolMetadataUpdater.class);

        builder.service(ResourceProvider.class, CubeIDResourceProvider.class);
        builder.service(ResourceProvider.class, CubeResourceProvider.class);
    }

}
