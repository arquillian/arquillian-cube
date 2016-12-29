package org.arquillian.cube.docker.drone;

import org.arquillian.cube.spi.Cube;
import org.arquillian.cube.spi.CubeRegistry;
import org.jboss.arquillian.core.api.Event;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.test.spi.TestResult;
import org.jboss.arquillian.test.spi.event.suite.After;
import org.jboss.arquillian.test.spi.event.suite.AfterSuite;

import java.nio.file.Path;

/**
 * Created by hemani on 12/27/16.
 */
public class VideoConversionLifecycleManager {

    @Inject
    Event<AfterSuite> afterSuiteEvent;

    Cube flv2mp4;

    public void startConversion(@Observes AfterSuite afterSuite, CubeDroneConfiguration cubeDroneConfiguration, CubeRegistry cubeRegistry) {

        initConversionCube(cubeRegistry);
        flv2mp4.start();
    }

    private void initConversionCube(CubeRegistry cubeRegistry) {
        if(flv2mp4 == null) {
            Cube conversionContainer = cubeRegistry.getCube(SeleniumContainers.CONVERSION_CONTAINER_NAME);

            if (conversionContainer == null) {
                throw new IllegalArgumentException("CONVERSION cube is not present in the registry.");

            }

            this.flv2mp4 = conversionContainer;
        }
    }

    public void stopContainer(@Observes After afterTestMethod, TestResult testResult, CubeDroneConfiguration cubeDroneConfiguration, SeleniumContainers seleniumContainers) {

        if (this.flv2mp4 != null) {

            /*Path finalLocation = null;
            if (shouldRecordOnlyOnFailure(testResult, cubeDroneConfiguration)) {
                finalLocation = moveFromVolumeFolderToBuildDirectory(afterTestMethod, cubeDroneConfiguration, seleniumContainers);
            } else {
                if (shouldRecordAlways(cubeDroneConfiguration)) {
                    finalLocation = moveFromVolumeFolderToBuildDirectory(afterTestMethod, cubeDroneConfiguration, seleniumContainers);
                }
            }
*/
            flv2mp4.stop();
            flv2mp4.destroy();

            this.afterSuiteEvent.fire(new AfterSuite());
        }

    }

}
