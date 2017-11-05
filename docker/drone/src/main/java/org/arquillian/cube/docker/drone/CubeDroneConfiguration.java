package org.arquillian.cube.docker.drone;

import java.util.Map;

/**
 * Configuration for cube drone extension.
 */
public class CubeDroneConfiguration {

    /**
     * Final directory where recordings are moved after execution.
     */
    private String finalDirectory;
    /**
     * Record mode to decide if not recording, only when failure or always.
     */
    private RecordMode recordMode = RecordMode.ALL;
    /**
     * Docker image to be used as custom browser image instead of the official one.
     */
    private String browserImage = null;
    /**
     * Dockerfile location to be used to built custom docker image instead of the official one.
     * This property has preference over browserImage.
     *
     * @see org.arquillian.cube.docker.drone.CubeDroneConfiguration#browserImage
     */
    private String browserDockerfileLocation = null;
    /**
     * Strategy for naming of containers, either always the same name or static with a configurable
     * prefix or a randomly generated, unique name.
     */
    private ContainerNameStrategy containerNameStrategy = ContainerNameStrategy.STATIC;
    /**
     * Prefix for STATIC_PREFIX container name strategy.
     */
    private String containerNamePrefix = null;

    public static CubeDroneConfiguration fromMap(Map<String, String> config) {
        CubeDroneConfiguration cubeDroneConfiguration = new CubeDroneConfiguration();

        if (config.containsKey("recordingMode")) {
            cubeDroneConfiguration.recordMode = RecordMode.valueOf(config.get("recordingMode"));
        }

        if (config.containsKey("videoOutput")) {
            cubeDroneConfiguration.finalDirectory = config.get("videoOutput");
        }

        if (config.containsKey("browserImage")) {
            cubeDroneConfiguration.browserImage = config.get("browserImage");
        }

        if (config.containsKey("browserDockerfileLocation")) {
            cubeDroneConfiguration.browserDockerfileLocation = config.get("browserDockerfileLocation");
        }

        if (config.containsKey("containerNameStrategy")) {
            cubeDroneConfiguration.containerNameStrategy = ContainerNameStrategy.valueOf(config.get("containerNameStrategy"));
        }

        if (config.containsKey("containerNamePrefix")) {
            cubeDroneConfiguration.containerNamePrefix = config.get("containerNamePrefix");
        }

        return cubeDroneConfiguration;
    }

    public boolean isRecordOnFailure() {
        return recordMode == RecordMode.ONLY_FAILING;
    }

    public boolean isRecording() {
        return recordMode != RecordMode.NONE;
    }

    public RecordMode getRecordMode() {
        return recordMode;
    }

    public boolean isVideoOutputDirectorySet() {
        return finalDirectory != null;
    }

    public String getFinalDirectory() {
        return finalDirectory;
    }

    public boolean isBrowserImageSet() {
        return this.browserImage != null && !this.browserImage.isEmpty();
    }

    public boolean isBrowserDockerfileDirectorySet() {
        return this.browserDockerfileLocation != null && !this.browserDockerfileLocation.isEmpty();
    }

    public String getBrowserImage() {
        return browserImage;
    }

    public String getBrowserDockerfileLocation() {
        return browserDockerfileLocation;
    }
    
    public ContainerNameStrategy getContainerNameStrategy(){
        return this.containerNameStrategy;
    }

    public String getContainerNamePrefix(){
        return this.containerNamePrefix;
    }
    
    public static enum RecordMode {
        ALL, ONLY_FAILING, NONE;
    }
    
    public static enum ContainerNameStrategy {
        STATIC, STATIC_PREFIX, RANDOM;
    }
}
