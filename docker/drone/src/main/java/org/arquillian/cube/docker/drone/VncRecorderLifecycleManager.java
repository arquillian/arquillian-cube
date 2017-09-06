package org.arquillian.cube.docker.drone;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import org.arquillian.cube.docker.drone.event.AfterVideoRecorded;
import org.arquillian.cube.docker.drone.util.VideoFileDestination;
import org.arquillian.cube.spi.Cube;
import org.arquillian.cube.spi.CubeRegistry;
import org.jboss.arquillian.core.api.Event;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.test.spi.TestResult;
import org.jboss.arquillian.test.spi.event.suite.After;
import org.jboss.arquillian.test.spi.event.suite.Before;

/**
 * Vnc Cube is set to manual so we can start and stop before and after each test method.
 * Since recording is finished when container is stopped and we want to have one video per method, each time image is
 * started and stopped.
 */
public class VncRecorderLifecycleManager {

    @Inject
    Event<AfterVideoRecorded> afterVideoRecordedEvent;

    @Inject
    Instance<SeleniumContainers> seleniumContainersInstance;
    
    Cube vnc;

    public void startRecording(@Observes Before beforeTestMethod, CubeDroneConfiguration cubeDroneConfiguration,
        CubeRegistry cubeRegistry) {

        if (cubeDroneConfiguration.isRecording()) {

            // lazy init
            initVncCube(cubeRegistry);

            vnc.create();
            vnc.start();
        }
    }

    private void initVncCube(CubeRegistry cubeRegistry) {
        if (vnc == null) {
            SeleniumContainers seleniumContainers = seleniumContainersInstance.get();
            
            Cube vncContainer = cubeRegistry.getCube(seleniumContainers.getVncContainerName());

            if (vncContainer == null) {
                throw new IllegalArgumentException("VNC cube is not present in registry.");
            }

            this.vnc = vncContainer;
        }
    }

    public void stopRecording(@Observes After afterTestMethod, TestResult testResult,
        CubeDroneConfiguration cubeDroneConfiguration, SeleniumContainers seleniumContainers) {

        if (this.vnc != null) {
            vnc.stop();
            
            Path finalLocation = null;
            if (shouldRecordOnlyOnFailure(testResult, cubeDroneConfiguration)) {
                finalLocation =
                    moveFromVolumeFolderToBuildDirectory(afterTestMethod, cubeDroneConfiguration, seleniumContainers);
            } else {
                if (shouldRecordAlways(cubeDroneConfiguration)) {
                    finalLocation =
                        moveFromVolumeFolderToBuildDirectory(afterTestMethod, cubeDroneConfiguration, seleniumContainers);
                }
            }

            vnc.destroy();

            this.afterVideoRecordedEvent.fire(new AfterVideoRecorded(afterTestMethod, finalLocation));
        }
    }

    private boolean shouldRecordAlways(CubeDroneConfiguration cubeDroneConfiguration) {
        return cubeDroneConfiguration.isRecording() && !cubeDroneConfiguration.isRecordOnFailure();
    }

    private boolean shouldRecordOnlyOnFailure(TestResult testResult, CubeDroneConfiguration cubeDroneConfiguration) {
        return cubeDroneConfiguration.isRecordOnFailure() && testResult.getStatus() == TestResult.Status.FAILED;
    }

    private Path moveFromVolumeFolderToBuildDirectory(After afterTestMethod,
        CubeDroneConfiguration cubeDroneConfiguration, SeleniumContainers seleniumContainers) {
        try {
            final Path finalLocation = getFinalLocation(cubeDroneConfiguration, afterTestMethod);
            Files.move(seleniumContainers.getVideoRecordingFile(), finalLocation, StandardCopyOption.REPLACE_EXISTING);
            return finalLocation;
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private Path getFinalLocation(CubeDroneConfiguration cubeDroneConfiguration, After afterTestMethod) {
        return VideoFileDestination.getFinalLocation(afterTestMethod, cubeDroneConfiguration);
    }
}
