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
     * Dockerfiel location to be used to built custom docker image instead of the official one.
     * This property has preference over browserImage.
     *
     * @see org.arquillian.cube.docker.drone.CubeDroneConfiguration#browserImage
     */
    private String browserDockerfileDirectory = null;

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
        return this.browserDockerfileDirectory != null && !this.browserDockerfileDirectory.isEmpty();
    }

    public String getBrowserImage() {
        return browserImage;
    }

    public String getBrowserDockerfileDirectory() {
        return browserDockerfileDirectory;
    }

    public static enum RecordMode {
        ALL, ONLY_FAILING, NONE;
    }

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

        if (config.containsKey("browserDockerfileDirectory")) {
            cubeDroneConfiguration.browserDockerfileDirectory = config.get("browserDockerfileDirectory");
        }

        return cubeDroneConfiguration;
    }

}
