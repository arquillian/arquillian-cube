package org.arquillian.cube.docker.impl.util;

public enum OperatingSystemFamily {
    LINUX("unix:///var/run/docker.sock", false),
    WINDOWS("tcp://" + AbstractCliInternetAddressResolver.DOCKERHOST_TAG + ":2376", true),
    UNIX("unix:///var/run/docker.sock", false),
    DIND("unix:///var/run/docker.sock", false),
    DEC_OS("unix:///var/run/docker.sock", false),
    MAC("tcp://" + AbstractCliInternetAddressResolver.DOCKERHOST_TAG + ":2376", true),
    MACHINE("tcp://" + AbstractCliInternetAddressResolver.DOCKERHOST_TAG + ":2376", true),
    UNKNOWN("tcp://" + AbstractCliInternetAddressResolver.DOCKERHOST_TAG + ":2376", true);

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
