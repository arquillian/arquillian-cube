package org.arquillian.cube;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * HostPort annotation is used to inject the bound port to a container object.
 * Typically a docker image has an exposed port which is bound to a binding port (to access from outside).
 * With this annotation you set the exposed port, and Cube will inject the bind port associated to this exposed port.
 * <p>
 * For example given: 2222->22/tcp and using:
 *
 * @HostPort(22) int port;
 * <p>
 * Port value will be 2222.
 */
@Target({FIELD, PARAMETER})
@Retention(RUNTIME)
@Documented
public @interface HostPort {

    /**
     * Sets the cube name where you want to get the exposed port. Only useful in case of not using Container Object
     * pattern.
     *
     * @return Cube Name
     */
    String containerName() default "";

    int value() default 0;
}
