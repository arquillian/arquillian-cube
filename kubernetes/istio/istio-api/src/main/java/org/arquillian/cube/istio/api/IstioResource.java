package org.arquillian.cube.istio.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to be used to populate Istio resource into the cluster.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Repeatable(IstioResource.List.class)
public @interface IstioResource {

    /**
     * Location of Istio Resource file. If it starts with http(s) or file the value is treated as URL.
     * If location is prefixed with classpath, then the resource is considered to be located at classpath.
     * If it is not prefixed, then the resource is considered to be the content text.
     *
     * This String also supports expressions like ${property:defaultValue} where {@code property} is resolved against system property, if not set then environment variable,
     * and if not set the default value (if specified) is returned.
     *
     * @return Istio Resource location.
     */
    String value();

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
    @interface List {
        IstioResource[] value();
    }

}
