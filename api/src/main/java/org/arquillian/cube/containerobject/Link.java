package org.arquillian.cube.containerobject;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Documented
public @interface Link {

    public static final String DEFAULT_VALUE = "";
    String value() default DEFAULT_VALUE;

}
