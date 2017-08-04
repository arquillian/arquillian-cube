package org.arquillian.cube.openshift.impl.enricher;

import io.fabric8.openshift.api.model.v2_5.Route;
import org.arquillian.cube.impl.util.ReflectionUtil;
import org.arquillian.cube.kubernetes.api.Configuration;
import org.arquillian.cube.openshift.impl.client.CubeOpenShiftConfiguration;
import org.arquillian.cube.openshift.impl.client.OpenShiftClient;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.test.spi.TestEnricher;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;

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
            try {
                if (!field.isAccessible()) {
                    field.setAccessible(true);
                }
                field.set(testCase, lookup(getRouteURLAnnotation(field.getAnnotations())));
            } catch (Exception e) {
                throw new RuntimeException("Could not set RouteURL value on field " + field, e);
            }
        }
    }

    @Override
    public Object[] resolve(Method method) {
        Object[] values = new Object[method.getParameterTypes().length];
        Class<?>[] parameterTypes = method.getParameterTypes();
        for (int i = 0; i < parameterTypes.length; i++) {
            RouteURL routeURL = getRouteURLAnnotation(method.getParameterAnnotations()[i]);
            if (routeURL != null) {
                values[i] = lookup(routeURL);
            }
        }
        return values;
    }

    private RouteURL getRouteURLAnnotation(Annotation[] annotations) {
        for (Annotation annotation : annotations) {
            if (annotation.annotationType() == RouteURL.class) {
                return (RouteURL) annotation;
            }
        }
        return null;
    }

    private URL lookup(RouteURL routeURL) {

        final String routeName = routeURL.value();
        if (routeURL == null || routeName == null || routeName.length() == 0) {
            throw new NullPointerException("RouteURL is null, must specify a route name!");
        }

        final CubeOpenShiftConfiguration config = (CubeOpenShiftConfiguration) configurationInstance.get();
        if (config == null) {
            throw new NullPointerException("CubeOpenShiftConfiguration is null.");
        }

        final String routerAddress = config.getRouterHost();
        if (routerAddress == null || routerAddress.length() == 0) {
            throw new IllegalArgumentException("Must specify routerHost!");
        }

        final OpenShiftClient client = clientInstance.get();
        final Route route = client.getClient().routes().inNamespace(config.getNamespace()).withName(routeName).get();
        if (route == null) {
            throw new IllegalArgumentException("Could not resolve route: " + routeName);
        }

        final String protocol = route.getSpec().getTls() == null ? "http" : "https";
        final int port = protocol.equals("http") ? config.getOpenshiftRouterHttpPort() : config.getOpenshiftRouterHttpsPort();

        try {
            return new URL(protocol, route.getSpec().getHost(), port, "/");
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Unable to create route URL", e);
        }
    }
}
