package org.arquillian.cube.spi.requirement;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Requires annotation is used to express {@link Requirement}.
 * Can be used directly on a type or method, but also can be used to annotate other annotations.
 * When used to annotate an other annotation, the target annotation defines the requirement context (which is passed to
 * the {@link Requirement}.
 * In all other cases, there is no context.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.ANNOTATION_TYPE, ElementType.TYPE, ElementType.METHOD})
public @interface Requires {
    

    Class<? extends Requirement>[] value() default {};
}
