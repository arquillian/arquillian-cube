package org.arquillian.cube.containerobject;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.TYPE})
@Documented
public @interface Cube {

    String DEFAULT_VALUE = "";
    String[] DEFAULT_PORT_BINDING = new String[] {};
    int[] DEFAULT_AWAIT_PORT_BINDING = new int[] {};

    String value() default DEFAULT_VALUE;

    String[] portBinding() default {};

    int[] awaitPorts() default {};

    ConnectionMode connectionMode() default ConnectionMode.START_AND_STOP_AROUND_CLASS;
}
