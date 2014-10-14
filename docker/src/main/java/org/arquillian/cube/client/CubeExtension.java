package org.arquillian.cube.client;

import org.jboss.arquillian.core.spi.LoadableExtension;

public class CubeExtension implements LoadableExtension {

    @Override
    public void register(ExtensionBuilder builder) {
        builder.observer(CubeConfigurator.class);
        builder.observer(CubeLifecycle.class);
    }

}
