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

        return cubeDroneConfiguration;
    }

}
