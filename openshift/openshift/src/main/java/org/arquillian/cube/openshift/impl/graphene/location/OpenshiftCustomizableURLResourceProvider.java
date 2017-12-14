package org.arquillian.cube.openshift.impl.graphene.location;

import io.fabric8.openshift.api.model.v3_1.Route;
import org.arquillian.cube.openshift.impl.client.OpenShiftClient;
import org.arquillian.cube.kubernetes.api.Configuration;
import org.arquillian.cube.openshift.impl.client.CubeOpenShiftConfiguration;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.graphene.spi.configuration.GrapheneConfiguration;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.test.spi.enricher.resource.ResourceProvider;

import java.lang.annotation.Annotation;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;

public class OpenshiftCustomizableURLResourceProvider implements ResourceProvider {

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
            return resolveUrl();
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private URL resolveUrl() throws MalformedURLException {
        final GrapheneConfiguration grapheneConfiguration = this.grapheneConfiguration.get();

        final String configuredUrl = grapheneConfiguration.getUrl();

        if (configuredUrl != null && !configuredUrl.isEmpty()) {
            return getRoute(new URL(configuredUrl).getHost()).get();
        }
        return getRoute().get();
    }

    private Optional<URL> getRoute(String routeName) {
        final OpenShiftClient client = clientInstance.get();

        final CubeOpenShiftConfiguration config = (CubeOpenShiftConfiguration) configurationInstance.get();

        Route route = client.getClient().routes().inNamespace(config.getNamespace()).withName(routeName).get();

        return route != null ? createUrlFromRoute(route) : Optional.empty();
    }

    private Optional<URL> getRoute() {
        final OpenShiftClient client = clientInstance.get();

        final CubeOpenShiftConfiguration config = (CubeOpenShiftConfiguration) configurationInstance.get();

        Optional<Route> optionalRoute = client.getClient().routes().inNamespace(config.getNamespace())
            .list().getItems()
            .stream()
            .findFirst();

        return optionalRoute
            .map(this::createUrlFromRoute)
            .orElse(Optional.empty());
    }

    private Optional<URL> createUrlFromRoute(Route route) {
        try {
            final String protocol = route.getSpec().getTls() == null ? "http" : "https";
            final String path = route.getSpec().getPath() == null ? "" : route.getSpec().getPath();
            return Optional.of(
                new URL(protocol, route.getSpec().getHost(), resolvePort(protocol), path));
        } catch (MalformedURLException e) {
            throw new IllegalStateException(e);
        }
    }

    private int resolvePort(String protocol) {
        if ("http".equals(protocol)) {
            return 80;
        } else {
            return 443;
        }
    }
}
