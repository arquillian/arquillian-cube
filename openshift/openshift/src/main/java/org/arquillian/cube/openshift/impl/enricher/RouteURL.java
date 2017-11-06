package org.arquillian.cube.openshift.impl.enricher;

import javax.inject.Qualifier;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Provides a URL of given OpenShift route. If the route is not resolvable, you need to set the {@code routerHost} setting
 * to the IP address of the OpenShift router.
 *
 * @author Rob Cernich
 */
@Qualifier
@Documented
@Retention(RUNTIME)
@Target({ElementType.FIELD, ElementType.PARAMETER })
public @interface RouteURL {
    /**
     * @return the route name
     */
    String value() default "";

    /**
     * The {@code path} part of the URL that will be appended to the root route URL. Useful if you only need a single URL
     * to the application behind the route and don't want to construct that URL programmatically. Defaults to {@code /}.
     */
    String path() default "/";
}
