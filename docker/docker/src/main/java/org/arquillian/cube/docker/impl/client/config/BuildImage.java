package org.arquillian.cube.docker.impl.client.config;

public class BuildImage {
    private String dockerfileLocation;
    private String dockerfileName; // both??
    private boolean noCache = false;
    private boolean remove = false;

    protected BuildImage() {}

    public BuildImage(String dockerfileLocation, String dockerfileName, boolean noCache, boolean remove) {
        this.dockerfileLocation = dockerfileLocation;
        this.dockerfileName = dockerfileName;
        this.noCache = noCache;
        this.remove = remove;
    }

    public String getDockerfileLocation() {
        return dockerfileLocation;
    }

    public void setDockerfileLocation(String dockerFileLocation) {
        this.dockerfileLocation = dockerFileLocation;
    }

    public String getDockerfileName() {
        return dockerfileName;
    }

    public void setDockerfileName(String dockerFileName) {
        this.dockerfileName = dockerFileName;
    }

    public boolean isNoCache() {
        return noCache;
    }

    public void setNoCache(boolean noCache) {
        this.noCache = noCache;
    }

    public boolean isRemove() {
        return remove;
    }

    public void setRemove(boolean remove) {
        this.remove = remove;
    }
}
