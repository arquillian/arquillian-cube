package org.arquillian.cube.docker.drone;

import org.arquillian.cube.spi.Cube;
import org.arquillian.cube.spi.CubeRegistry;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.test.spi.TestResult;
import org.jboss.arquillian.test.spi.event.suite.After;
import org.jboss.arquillian.test.spi.event.suite.Before;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * Vnc Cube is set to manual so we can start and stop before and after each test method.
 * Since recording is finished when container is stopped and we want to have one video per method, each time image is started and stopped.
 */
public class VncRecorderLifecycleManager {

    private static final File DEFAULT_LOCATION_OUTPUT_MAVEN = new File("target");
    private static final File DEFAULT_LOCATION_OUTPUT_GRADLE = new File("build");

    private static final File MAVEN_REPORT_DIR = new File(DEFAULT_LOCATION_OUTPUT_MAVEN, "surefire-reports");
    private static final File GRADLE_REPORT_DIR = new File(DEFAULT_LOCATION_OUTPUT_GRADLE, "test-results");

    Cube vnc;

    public void startRecording(@Observes Before beforeTestMethod, CubeDroneConfiguration cubeDroneConfiguration, CubeRegistry cubeRegistry) {

        if (cubeDroneConfiguration.isRecording()) {

            // lazy init
            initVncCube(cubeRegistry);

            vnc.create();
            vnc.start();

        }

    }

    private void initVncCube(CubeRegistry cubeRegistry) {
        if (vnc == null) {
            Cube vncContainer = cubeRegistry.getCube(SeleniumContainers.VNC_CONTAINER_NAME);

            if (vncContainer == null) {
                throw new IllegalArgumentException("VNC cube is not present in registry.");
            }

            this.vnc = vncContainer;
        }
    }

    public void stopRecording(@Observes After afterTestMethod, TestResult testResult, CubeDroneConfiguration cubeDroneConfiguration, SeleniumContainers seleniumContainers) {

        if (this.vnc != null) {

            if (cubeDroneConfiguration.isRecordingOnlyFailing() && testResult.getStatus() == TestResult.Status.FAILED) {
                moveFromVolumeFolderToBuildDirectory(afterTestMethod, cubeDroneConfiguration, seleniumContainers);
            } else {
                // RECORD ALWAYS
                if (cubeDroneConfiguration.isRecording() && !cubeDroneConfiguration.isRecordingOnlyFailing()) {
                    moveFromVolumeFolderToBuildDirectory(afterTestMethod, cubeDroneConfiguration, seleniumContainers);
                }
            }

            vnc.stop();
            vnc.destroy();
        }

    }

    private void moveFromVolumeFolderToBuildDirectory(After afterTestMethod, CubeDroneConfiguration cubeDroneConfiguration, SeleniumContainers seleniumContainers) {
        try {
            Files.move(seleniumContainers.getVideoRecordingFile(), getFinalLocation(cubeDroneConfiguration, afterTestMethod), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private Path getFinalLocation(CubeDroneConfiguration cubeDroneConfiguration, After afterTestMethod) {
        return resolveTargetDirectory(cubeDroneConfiguration).resolve(getFinalVideoName(afterTestMethod));
    }

    private Path resolveTargetDirectory(CubeDroneConfiguration cubeDroneConfiguration) {
        if (cubeDroneConfiguration.isVideoOutputDirectorySet()) {
            return Paths.get(cubeDroneConfiguration.getFinalDirectory());
        } else {
            if (MAVEN_REPORT_DIR.exists()) {
                return MAVEN_REPORT_DIR.toPath();
            } else {
                if (GRADLE_REPORT_DIR.exists()) {
                    return GRADLE_REPORT_DIR.toPath();
                } else {
                    if (DEFAULT_LOCATION_OUTPUT_GRADLE.exists()) {
                        return DEFAULT_LOCATION_OUTPUT_GRADLE.toPath();
                    } else {
                        if (DEFAULT_LOCATION_OUTPUT_MAVEN.exists()) {
                            return DEFAULT_LOCATION_OUTPUT_MAVEN.toPath();
                        }
                    }
                }
            }
        }

        if (!DEFAULT_LOCATION_OUTPUT_MAVEN.mkdirs()) {
            throw new IllegalArgumentException("Couldn't create directory for storing videos");
        }
        return DEFAULT_LOCATION_OUTPUT_MAVEN.toPath();
    }

    private String getFinalVideoName(After afterTestMethod) {

        final String className = afterTestMethod.getTestClass().getName().replace('.', '_');
        final String methodName = afterTestMethod.getTestMethod().getName();

        return className + "_" + methodName + ".flv";

    }

}
