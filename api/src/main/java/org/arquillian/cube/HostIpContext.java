package org.arquillian.cube;

public class HostIpContext {

    private String ip;

    public HostIpContext(String ip) {
        this.ip = ip;
    }

    public String getHost() {
        return this.ip;
    }
}
