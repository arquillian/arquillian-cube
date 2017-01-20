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

    public static final String DEFAULT_VALUE = "";
    public static final String[] DEFAULT_PORT_BINDING = new String[] {};
    public static final int[] DEFAULT_AWAIT_PORT_BINDING = new int[] {};

    String value() default DEFAULT_VALUE;
    String[] portBinding() default {};
    int[] awaitPorts() default {};
}
