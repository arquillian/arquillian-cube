package org.arquillian.cube.impl.util;

public enum OperatingSystemFamily {
    LINUX("unix:///var/run/docker.sock"),
    WINDOWS("https://boot2docker:2376"),
    UNIX("unix:///var/run/docker.sock"),
    DEC_OS("unix:///var/run/docker.sock"),
    MAC("https://boot2docker:2376"),
    UNKNOWN("https://boot2docker:2376");

    private final String serverUri;

    private OperatingSystemFamily(String serverUri) {
        this.serverUri = serverUri;
    }
    public String getServerUri() {
        return serverUri;
    }
}
