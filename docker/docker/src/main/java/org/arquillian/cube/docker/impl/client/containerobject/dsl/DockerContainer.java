package org.arquillian.cube.docker.impl.client.containerobject.dsl;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
@Documented
/**
 * Annotation to mark a Container object as is.
 * @see Container
 */
public @interface DockerContainer {

    /**
     * Order to start the container, the bigger the number, sooner is started.
     */
    int order() default 0;
}
