package org.arquillian.cube.kubernetes.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER})
@Documented
// @Repeatable(WithLabels.class) // TODO: once we are on Java 8
public @interface WithLabel {

    String name();
    String value();

}
