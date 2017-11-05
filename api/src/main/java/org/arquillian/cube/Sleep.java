package org.arquillian.cube;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target({TYPE})
@Retention(RUNTIME)
@Documented
public @interface Sleep {

    /**
     * Sets the sleep time. By default if nothing is specified the value is considered in milliseconds, but you can also use a timespan form such as 1m30s
     * @return Sleeping time.
     */
    String value();

}
