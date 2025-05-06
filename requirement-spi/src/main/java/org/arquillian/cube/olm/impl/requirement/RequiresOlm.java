package org.arquillian.cube.olm.impl.requirement;

import org.arquillian.cube.spi.requirement.Constraint;
import org.arquillian.cube.spi.requirement.Requires;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks tests that require Operator Lifecycle Manager
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@Requires(Constraint.class)
public @interface RequiresOlm {
}
