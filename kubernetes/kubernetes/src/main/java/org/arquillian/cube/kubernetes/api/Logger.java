package org.arquillian.cube.kubernetes.api;

public interface Logger extends WithToImmutable<Logger> {

    void info(String msg);

    void warn(String msg);

    void error(String msg);

    void status(String msg);
}
