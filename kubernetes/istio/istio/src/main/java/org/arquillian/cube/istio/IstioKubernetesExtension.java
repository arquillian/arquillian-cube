package org.arquillian.cube.istio;

import org.arquillian.cube.istio.impl.IstioAssistantCreator;
import org.arquillian.cube.istio.impl.IstioClientCreator;
import org.arquillian.cube.istio.impl.IstioResourcesApplier;
import org.arquillian.cube.istio.impl.enricher.IstioAssistantResourceProvider;
import org.jboss.arquillian.core.spi.LoadableExtension;
import org.jboss.arquillian.test.spi.enricher.resource.ResourceProvider;

public class IstioKubernetesExtension implements LoadableExtension {
    @Override
    public void register(ExtensionBuilder extensionBuilder) {
        extensionBuilder
            .observer(IstioClientCreator.class)
            .observer(IstioResourcesApplier.class)
            .observer(IstioAssistantCreator.class)
            .service(ResourceProvider.class, IstioAssistantResourceProvider.class);
    }
}
