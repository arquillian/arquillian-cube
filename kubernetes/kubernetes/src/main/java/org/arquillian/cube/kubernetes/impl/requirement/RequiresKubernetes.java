package org.arquillian.cube.kubernetes.impl.requirement;

import org.arquillian.cube.spi.requirement.Requires;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@Requires(KubernetesRequirement.class)
public @interface RequiresKubernetes {

    String name() default "";
}
