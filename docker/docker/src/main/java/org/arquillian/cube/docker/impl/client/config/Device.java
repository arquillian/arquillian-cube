package org.arquillian.cube.docker.impl.client.config;

public class Device {
    private String pathOnHost;
    private String pathInContainer;
    private String cGroupPermissions;

    public Device() {
    }

    public Device(String pathOnHost, String pathInContainer, String cGroupPermissions) {
        super();
        this.pathOnHost = pathOnHost;
        this.pathInContainer = pathInContainer;
        this.cGroupPermissions = cGroupPermissions;
    }

    public String getPathOnHost() {
        return pathOnHost;
    }

    public void setPathOnHost(String pathOnHost) {
        this.pathOnHost = pathOnHost;
    }

    public String getPathInContainer() {
        return pathInContainer;
    }

    public void setPathInContainer(String pathInContainer) {
        this.pathInContainer = pathInContainer;
    }

    public String getcGroupPermissions() {
        return cGroupPermissions;
    }

    public void setcGroupPermissions(String cGroupPermissions) {
        this.cGroupPermissions = cGroupPermissions;
    }
}
