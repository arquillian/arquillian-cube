package org.arquillian.cube;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target({TYPE})
@Retention(RUNTIME)
@Documented
public @interface HealthCheck {

    String value() default "/";
    String schema() default "http";
    int port() default 8080;
    String containerName() default "";
    String method() default "GET";
    int responseCode() default 200;
    int iterations() default 40;
    String interval() default "500ms";
    String timeout() default "2s";

}
