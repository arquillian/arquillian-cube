package org.arquillian.cube.openshift.impl.graphene.location;

import org.arquillian.cube.kubernetes.api.Configuration;
import org.arquillian.cube.openshift.impl.client.OpenShiftRouteLocator;
import org.arquillian.cube.openshift.impl.client.OpenShiftClient;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.graphene.spi.configuration.GrapheneConfiguration;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.test.spi.enricher.resource.ResourceProvider;

import java.lang.annotation.Annotation;
import java.net.MalformedURLException;
import java.net.URL;

import static org.arquillian.cube.openshift.impl.client.ResourceUtil.awaitRoute;

public class OpenShiftCustomizableURLResourceProvider implements ResourceProvider {

    @Inject
    private Instance<GrapheneConfiguration> grapheneConfiguration;

    @Inject
    private Instance<OpenShiftClient> clientInstance;

    @Inject
    private Instance<Configuration> configurationInstance;

    @Override
    public boolean canProvide(Class<?> type) {
        return URL.class.isAssignableFrom(type);
    }

    @Override
    public Object lookup(ArquillianResource resource, Annotation... qualifiers) {
        try {
            final URL routeUrl = resolveUrl();
            awaitRoute(routeUrl);
            return routeUrl;
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private URL resolveUrl() throws MalformedURLException {
        final GrapheneConfiguration grapheneConfiguration = this.grapheneConfiguration.get();
        final OpenShiftRouteLocator openShiftRouteLocator = new OpenShiftRouteLocator(clientInstance, configurationInstance);

        final String configuredUrl = grapheneConfiguration.getUrl();
        if (configuredUrl != null && !configuredUrl.isEmpty()) {
            return openShiftRouteLocator.getRoute(new URL(configuredUrl));
        }
        return openShiftRouteLocator.getRoute();
    }
}
