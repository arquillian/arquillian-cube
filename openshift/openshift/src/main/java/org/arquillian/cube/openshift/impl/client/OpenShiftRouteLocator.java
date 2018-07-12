package org.arquillian.cube.openshift.impl.client;

import io.fabric8.openshift.api.model.v4_0.Route;
import org.arquillian.cube.kubernetes.api.Configuration;
import org.jboss.arquillian.core.api.Instance;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;

public class OpenShiftRouteLocator {

    private Instance<OpenShiftClient> clientInstance;
    private Instance<Configuration> configurationInstance;

    public OpenShiftRouteLocator(Instance<OpenShiftClient> clientInstance, Instance<Configuration> configurationInstance) {
        this.clientInstance = clientInstance;
        this.configurationInstance = configurationInstance;
    }

    public URL getRoute(URL routeUrl) {
        final CubeOpenShiftConfiguration config = getCubeOpenShiftConfiguration();
        final OpenShiftClient client = clientInstance.get();

        final String routeName = routeUrl.getHost();
        Route route = client.getClient().routes().inNamespace(config.getNamespace()).withName(routeName).get();
        if (route == null) {
            return routeUrl;
        }
        return createUrlFromRoute(route);
    }

    public URL getRoute() {
        final CubeOpenShiftConfiguration config = getCubeOpenShiftConfiguration();
        final OpenShiftClient client = clientInstance.get();

        Optional<Route> optionalRoute = client.getClient().routes().inNamespace(config.getNamespace())
            .list().getItems()
            .stream()
            .findFirst();

        return optionalRoute
            .map(OpenShiftRouteLocator::createUrlFromRoute)
            .orElseThrow(() -> new NullPointerException("No route defined."));
    }

    private CubeOpenShiftConfiguration getCubeOpenShiftConfiguration() {
        final CubeOpenShiftConfiguration config = (CubeOpenShiftConfiguration) configurationInstance.get();
        if (config == null) {
            throw new NullPointerException("CubeOpenShiftConfiguration is null.");
        }
        return config;
    }

    static URL createUrlFromRoute(Route route) {
        try {
            final String protocol = route.getSpec().getTls() == null ? "http" : "https";
            final String path = route.getSpec().getPath() == null ? "" : route.getSpec().getPath();
            return new URL(protocol, route.getSpec().getHost(), resolvePort(protocol), path);
        } catch (MalformedURLException e) {
            throw new IllegalStateException(e);
        }
    }

    private static int resolvePort(String protocol) {
        if ("http".equals(protocol)) {
            return 80;
        } else {
            return 443;
        }
    }
}
