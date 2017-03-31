package org.arquillian.cube.containerobject;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
public @interface Image {

    public static final String DEFAULT_VALUE = "";

    String value() default DEFAULT_VALUE;
}
