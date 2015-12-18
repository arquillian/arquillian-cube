package org.arquillian.cube.docker.impl.client.config;

public class Copy {

    private String from;
    private String to;

    public Copy() {
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }
}
