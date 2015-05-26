package org.arquillian.cube.docker.impl.util;

public enum OperatingSystemFamily {
    LINUX("unix:///var/run/docker.sock", false),
    WINDOWS("https://boot2docker:2376", true),
    UNIX("unix:///var/run/docker.sock", false),
    DEC_OS("unix:///var/run/docker.sock", false),
    MAC("https://boot2docker:2376", true),
    UNKNOWN("https://boot2docker:2376", true);

    private final String serverUri;
    private final boolean boot2Docker;

    private OperatingSystemFamily(String serverUri, boolean boot2Docker) {
        this.serverUri = serverUri;
        this.boot2Docker = boot2Docker;
    }
    public String getServerUri() {
        return serverUri;
    }
    public boolean isBoot2Docker() {
        return this.boot2Docker;
    }
}
