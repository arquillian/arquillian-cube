package org.arquillian.cube.impl.client.container.remote;

import org.arquillian.cube.impl.client.enricher.CubeControllerProvider;
import org.jboss.arquillian.container.test.spi.RemoteLoadableExtension;
import org.jboss.arquillian.test.spi.enricher.resource.ResourceProvider;

public class CubeRemoteExtension implements RemoteLoadableExtension {

    @Override
    public void register(ExtensionBuilder builder) {
        builder.observer(ContainerCubeControllerCreator.class);
        builder.service(ResourceProvider.class, CubeControllerProvider.class);
        builder.service(ResourceProvider.class, ContainerCubeIDProvider.class);
    }
}
