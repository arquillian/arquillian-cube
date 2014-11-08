package org.arquillian.cube.client;

import org.jboss.arquillian.core.spi.LoadableExtension;
import org.jboss.arquillian.test.spi.TestEnricher;

public class CubeExtension implements LoadableExtension {

    @Override
    public void register(ExtensionBuilder builder) {
        builder.observer(CubeConfigurator.class);
        builder.observer(CubeLifecycle.class);
        builder.observer(ProtocolMetadataUpdater.class);

        builder.service(TestEnricher.class, ContainerEnricher.class);
        builder.service(TestEnricher.class, CubeEnricher.class);
    }

}
