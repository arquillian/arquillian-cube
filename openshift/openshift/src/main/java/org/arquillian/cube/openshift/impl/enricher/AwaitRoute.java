package org.arquillian.cube.openshift.impl.enricher;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

import javax.inject.Qualifier;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Used inside {@link RouteURL} to configure awating the route URL to be available.
 * Availability is defined as returning a known good HTTP status code.
 */
@Qualifier
@Documented
@Retention(RUNTIME)
@Target({})
public @interface AwaitRoute {
    /**
     * Path that should be appended to the route URL for checking route availability.
     * Useful when there's nothing exposed directly at the root route URL, only on some paths below.
     * Defaults to {@code /}.
     */
    String path() default "/";

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
