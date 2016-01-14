package org.arquillian.cube.docker.impl.client.config;

public class Log {

    private String to;
    private Boolean follow;
    private Boolean stdout;
    private Boolean stderr;
    private Boolean timestamps;
    private Integer tail;

    public Log() {
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public Boolean getFollow() {
        return follow;
    }

    public void setFollow(Boolean follow) {
        this.follow = follow;
    }

    public Boolean getStdout() {
        return stdout;
    }

    public void setStdout(Boolean stdout) {
        this.stdout = stdout;
    }

    public Boolean getStderr() {
        return stderr;
    }

    public void setStderr(Boolean stderr) {
        this.stderr = stderr;
    }

    public Boolean getTimestamps() {
        return timestamps;
    }

    public void setTimestamps(Boolean timestamps) {
        this.timestamps = timestamps;
    }

    public Integer getTail() {
        return tail;
    }

    public void setTail(Integer tail) {
        this.tail = tail;
    }
}
