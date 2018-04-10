package org.arquillian.cube.docker.impl.util;

public enum OperatingSystemFamily implements OperatingSystemFamilyInterface {
    LINUX("unix:///var/run/docker.sock", false),
    WINDOWS("https://" + AbstractCliInternetAddressResolver.DOCKERHOST_TAG + ":2376", true),
    WINDOWS_NPIPE("npipe:////./pipe/docker_engine", false),
    UNIX("unix:///var/run/docker.sock", false),
    DIND("unix:///var/run/docker.sock", false),
    DEC_OS("unix:///var/run/docker.sock", false),
    MAC("https://" + AbstractCliInternetAddressResolver.DOCKERHOST_TAG + ":2376", true),
    MACHINE("https://" + AbstractCliInternetAddressResolver.DOCKERHOST_TAG + ":2376", true),
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
