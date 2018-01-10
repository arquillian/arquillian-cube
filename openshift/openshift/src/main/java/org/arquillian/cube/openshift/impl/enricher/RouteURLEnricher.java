package org.arquillian.cube.openshift.impl.enricher;

import io.fabric8.openshift.api.model.v3_1.Route;
import io.fabric8.openshift.api.model.v3_1.RouteList;
import org.arquillian.cube.impl.util.ReflectionUtil;
import org.arquillian.cube.kubernetes.api.Configuration;
import org.arquillian.cube.openshift.impl.client.CubeOpenShiftConfiguration;
import org.arquillian.cube.openshift.impl.client.OpenShiftClient;
import org.jboss.arquillian.config.impl.extension.StringPropertyReplacer;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.test.spi.TestEnricher;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.arquillian.cube.openshift.impl.client.ResourceUtil.awaitRoute;

/**
 * RouteProxyProvider
 *
 * @author Rob Cernich
 */
public class RouteURLEnricher implements TestEnricher {

    @Inject
    private Instance<OpenShiftClient> clientInstance;

    @Inject
    private Instance<Configuration> configurationInstance;

    @Override
    public void enrich(Object testCase) {
        for (Field field : ReflectionUtil.getFieldsWithAnnotation(testCase.getClass(), RouteURL.class)) {
            Object url;
            AwaitRoute await;
            try {
                if (!field.isAccessible()) {
                    field.setAccessible(true);
                }
                RouteURL routeURL = getAnnotation(RouteURL.class, field.getAnnotations());
                url = lookup(routeURL, field.getType());
                field.set(testCase, url);
                await = getAnnotation(AwaitRoute.class, field.getAnnotations());
            } catch (Exception e) {
                throw new RuntimeException("Could not set RouteURL value on field " + field, e);
            }
            configureAwaitRoute(url, await);
        }
    }

    @Override
    public Object[] resolve(Method method) {
        Object[] values = new Object[method.getParameterTypes().length];
        Class<?>[] parameterTypes = method.getParameterTypes();
        for (int i = 0; i < parameterTypes.length; i++) {
            RouteURL routeURL = getAnnotation(RouteURL.class, method.getParameterAnnotations()[i]);
            if (routeURL != null) {
                Object url = lookup(routeURL, method.getParameterTypes()[i]);
                values[i] = url;
                AwaitRoute await = getAnnotation(AwaitRoute.class, method.getParameterAnnotations()[i]);
                configureAwaitRoute(url, await);
            }
        }
        return values;
    }

    private <T extends Annotation> T getAnnotation(Class<T> annotationClass, Annotation[] annotations) {
        for (Annotation annotation : annotations) {
            if (annotation.annotationType() == annotationClass) {
                return annotationClass.cast(annotation);
            }
        }
        return null;
    }

    private Object lookup(RouteURL routeURL, Class<?> returnType) {
        if (routeURL == null) {
            throw new NullPointerException("RouteURL is null!");
        }

        final String routeName = StringPropertyReplacer.replaceProperties(routeURL.value());
        if (routeName == null || routeName.length() == 0) {
            throw new NullPointerException("Route name is null, must specify a route name!");
        }

        final CubeOpenShiftConfiguration config = (CubeOpenShiftConfiguration) configurationInstance.get();
        if (config == null) {
            throw new NullPointerException("CubeOpenShiftConfiguration is null.");
        }

        final OpenShiftClient client = clientInstance.get();
        final Route route = client.getClient().routes().inNamespace(config.getNamespace()).withName(routeName).get();
        if (route == null) {
            List<Route> availableRoutes = client.getClient().routes().inNamespace(config.getNamespace()).list().getItems();
            throw new IllegalArgumentException("Could not resolve route: " + routeName + ". Available routes: " + availableRoutes.stream().map(r -> r.getMetadata().getName()).collect(Collectors.toList()));
        }

        final String protocol = route.getSpec().getTls() == null ? "http" : "https";
        // adding the port number to the URL if it's equal to the default port number for given protocol
        // is 100% correct, but unexpected
        final int port;
        if ("http".equals(protocol)) {
            port = config.getOpenshiftRouterHttpPort() == 80 ? -1 : config.getOpenshiftRouterHttpPort();
        } else {
            port = config.getOpenshiftRouterHttpsPort() == 443 ? -1 : config.getOpenshiftRouterHttpsPort();
        }

        try {
            URL url = new URL(protocol, route.getSpec().getHost(), port, routeURL.path());
            if (returnType == URL.class) {
                return url;
            } else if (returnType == URI.class) {
                return url.toURI();
            } else if (returnType == String.class) {
                return url.toExternalForm();
            } else {
                throw new IllegalArgumentException("Invalid route injection type (can only handle URL, URI, String): " + returnType.getName());
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to create route URL", e);
        }
    }

    private void configureAwaitRoute(Object route, AwaitRoute await) {
        // we _intentionally_ don't check if `configurationInstance.get().isWaitEnabled()` here;
        // even if awaiting readiness is disabled, we still want to await the route, because the user
        // explicitly opted into it, maybe because they want to workaround a Fabric8 Kubernetes Client
        // issue and rely on @AwaitRoute instead

        if (await == null) {
            return;
        }

        URL url;
        try {
            url = new URL(route.toString());
            if (!AwaitRoute.DEFAULT_PATH_FOR_ROUTE_AVAILABILITY_CHECK.equals(await.path())) {
                url = new URL(url.getProtocol(), url.getHost(), url.getPort(), await.path());
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        awaitRoute(url, await.timeout(), await.timeoutUnit(), await.statusCode());
    }
}
