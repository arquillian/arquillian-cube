package org.arquillian.cube.docker.restassured;

import io.restassured.builder.RequestSpecBuilder;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.test.spi.enricher.resource.ResourceProvider;

import java.lang.annotation.Annotation;

public class RequestSpecBuilderResourceProvider implements ResourceProvider {

    @Inject
    private Instance<RequestSpecBuilder> requestSpecBuilderInstance;

    @Override
    public boolean canProvide(Class<?> type) {
        return RequestSpecBuilder.class.isAssignableFrom(type);
    }

    @Override
    public Object lookup(ArquillianResource resource, Annotation... qualifiers) {

        RequestSpecBuilder requestSpecBuilder = requestSpecBuilderInstance.get();

        if (requestSpecBuilder == null) {
            throw new IllegalStateException("RequestSpecBuilder was not found.");
        }

        return requestSpecBuilder;
    }
}
