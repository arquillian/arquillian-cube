package org.arquillian.cube.docker.impl.client.config;

public class BeforeStop {

    private Copy copy;
    private Log log;

    public BeforeStop() {
    }

    public Copy getCopy() {
        return copy;
    }

    public void setCopy(Copy copy) {
        this.copy = copy;
    }

    public Log getLog() {
        return log;
    }

    public void setLog(Log log) {
        this.log = log;
    }
}
