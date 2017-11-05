package org.arquillian.cube.kubernetes.impl.log;

import org.arquillian.cube.kubernetes.api.Logger;

public class SimpleLogger implements Logger {
    @Override
    public void info(String msg) {
        System.out.println(msg);
    }

    @Override
    public void warn(String msg) {
        System.out.println(msg);
    }

    @Override
    public void error(String msg) {
        System.out.println(msg);
    }

    @Override
    public void status(String msg) {
        System.out.println(msg);
    }

    @Override
    public Logger toImmutable() {
        return this;
    }
}
