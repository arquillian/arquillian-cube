package org.arquillian.cube.docker.restassured;

import org.jboss.arquillian.core.spi.LoadableExtension;

public class RestAssuredExtension implements LoadableExtension {

    @Override
    public void register(ExtensionBuilder builder) {
        builder.observer(RestAssuredConfigurator.class)
               .observer(RestAssuredCustomizer.class);
    }
}
