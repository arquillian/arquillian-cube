package org.arquillian.cube.openshift.impl.enricher;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

import javax.inject.Qualifier;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * When this annotation is present alongside {@link RouteURL}, Arquillian Cube will wait
 * until the route becomes availabile. Availability is defined as returning a known good HTTP status code.
 */
@Qualifier
@Documented
@Retention(RUNTIME)
@Target({ElementType.FIELD, ElementType.PARAMETER })
public @interface AwaitRoute {
    String DEFAULT_PATH_FOR_ROUTE_AVAILABILITY_CHECK = "__DEFAULT PATH FOR ROUTE AVAILABILITY CHECK__";

    /**
     * Path that should be appended to the root route URL for checking route availability.
     * Useful when there's nothing exposed directly at the root route URL, only on some paths below.
     * Defaults to {@link RouteURL#path()}.
     * <p/>
     * If {@code RouteURL.path()} is set to a non-default value and this is also set to a non-default value,
     * then these two values are <b>not</b> combined. For example:
     * <pre>
     * &#64;RouteURL(value = "my-route", path = "/api/info")
     * &#64;AwaitRoute(path = "/api/health"))
     * private URL myUrl;
     * </pre>
     * In this case, the injected route URL will be {@code http://route.host/api/info}, but the URL
     * used for checking route availability will be {@code http://route.host/api/health}.
     */
    String path() default DEFAULT_PATH_FOR_ROUTE_AVAILABILITY_CHECK;

    /**
     * Set of HTTP status codes that are considered good. Defaults to a single value, {@code 200}.
     */
    int[] statusCode() default 200;

    /**
     * How long to wait for the route URL to become available. Defaults to 5 minutes.
     */
    int timeout() default 5;

    TimeUnit timeoutUnit() default TimeUnit.MINUTES;
}
