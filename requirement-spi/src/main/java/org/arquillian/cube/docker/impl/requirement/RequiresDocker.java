package org.arquillian.cube.docker.impl.requirement;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.arquillian.cube.spi.requirement.Constraint;
import org.arquillian.cube.spi.requirement.Requires;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@Requires(Constraint.class)
public @interface RequiresDocker {
}
