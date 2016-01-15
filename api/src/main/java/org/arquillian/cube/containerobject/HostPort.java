package org.arquillian.cube.containerobject;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * HostPort annotation is used to inject the bound port to a container object.
 * Typically a docker image has an exposed port which is bound to a binding port (to access from outside).
 * With this annotation you set the exposed port, and Cube will inject the bind port associated to this exposed port.
 *
 * For example given: 2222->22/tcp and using:
 *
 * @HostPort(22)
 * int port;
 *
 * Port value will be 2222.
 *
 */
@Target({FIELD})
@Retention(RUNTIME)
@Documented
public @interface HostPort {
    int value() default 0;
}
