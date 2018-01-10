package org.arquillian.cube.openshift.restassured;

import io.restassured.builder.RequestSpecBuilder;
import org.arquillian.cube.kubernetes.api.Configuration;
import org.arquillian.cube.openshift.impl.client.OpenShiftRouteLocator;
import org.arquillian.cube.openshift.impl.client.OpenShiftClient;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.test.spi.enricher.resource.ResourceProvider;

import java.lang.annotation.Annotation;
import java.net.MalformedURLException;
import java.net.URL;

import static org.arquillian.cube.openshift.impl.client.ResourceUtil.awaitRoute;

public class RequestSpecBuilderResourceProvider implements ResourceProvider {

    @Inject
    private Instance<OpenShiftClient> clientInstance;

    @Inject
    private Instance<Configuration> configurationInstance;

    @Inject
    private Instance<RestAssuredConfiguration> restAssuredConfigurationInstance;

    @Inject
    private Instance<RequestSpecBuilder> requestSpecBuilderInstance;

    @Override
    public boolean canProvide(Class<?> type) {
        return RequestSpecBuilder.class.isAssignableFrom(type);
    }

    @Override
    public Object lookup(ArquillianResource resource, Annotation... qualifiers) {
        try {
            final OpenShiftRouteLocator openShiftRouteLocator = new OpenShiftRouteLocator(clientInstance, configurationInstance);

            final RestAssuredConfiguration restAssuredConfiguration = this.restAssuredConfigurationInstance.get();
            final String baseUri = restAssuredConfiguration.getBaseUri();

            if (baseUri != null && !baseUri.isEmpty()) {
                return configureRequestSpecBuilder(openShiftRouteLocator.getRoute(new URL(baseUri)));
            } else {
                return configureRequestSpecBuilder(openShiftRouteLocator.getRoute());
            }
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private RequestSpecBuilder configureRequestSpecBuilder(URL route) {
        final RequestSpecBuilder requestSpecBuilder = requestSpecBuilderInstance.get();
        if (requestSpecBuilder == null) {
            throw new IllegalStateException("RequestSpecBuilder was not found.");
        }

        requestSpecBuilder.setBaseUri(route.getProtocol() + "://" + route.getHost());
        requestSpecBuilder.setPort(route.getPort());
        requestSpecBuilder.setBasePath(route.getPath());

        awaitRoute(route);
        return requestSpecBuilder;
    }
}
