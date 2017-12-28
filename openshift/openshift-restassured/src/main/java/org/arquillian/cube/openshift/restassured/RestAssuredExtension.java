package org.arquillian.cube.openshift.restassured;

import org.jboss.arquillian.core.spi.LoadableExtension;
import org.jboss.arquillian.test.spi.enricher.resource.ResourceProvider;

public class RestAssuredExtension implements LoadableExtension {

    @Override
    public void register(ExtensionBuilder builder) {
        builder.observer(RestAssuredConfigurator.class)
            .observer(RestAssuredCustomizer.class)
            .service(ResourceProvider.class, RequestSpecBuilderResourceProvider.class);
    }
}
