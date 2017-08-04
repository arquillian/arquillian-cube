package org.arquillian.cube.openshift.impl.enricher;

import javax.inject.Qualifier;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * OpenShiftRouter
 * <p/>
 * Used with @ArquillianResource URL objects. Provides a URL to the OpenShift
 * router configured on the ce-cube extension, u    e.g.
 * arq.extension.openshift.routerHost, or through arquillian.xml using routerHost parameter, setting the hostname appropriately.
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
}
