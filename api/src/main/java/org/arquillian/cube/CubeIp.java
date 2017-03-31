package org.arquillian.cube;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Returns internal ip of given container name or the container one depending on internal attribute value.
 */
@Target({FIELD, PARAMETER})
@Retention(RUNTIME)
@Documented
public @interface CubeIp {

    boolean internal() default true;

    String containerName() default "";
}
