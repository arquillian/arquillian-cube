package org.arquillian.cube;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target({FIELD, PARAMETER})
@Retention(RUNTIME)
@Documented
public @interface DockerUrl {

    /**
     * Sets the cube name where you want to get the exposed port. Only useful in case of not using Container Object
     * pattern.
     *
     * @return Cube Name
     */
    String containerName();

    int exposedPort();

    String protocol() default "http";

    String context() default "/";
}
