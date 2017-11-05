package org.arquillian.cube.kubernetes.impl.requirement;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.arquillian.cube.spi.requirement.Requires;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@Requires(KubernetesRequirement.class)
public @interface RequiresKubernetes {

    String name() default "";
}
