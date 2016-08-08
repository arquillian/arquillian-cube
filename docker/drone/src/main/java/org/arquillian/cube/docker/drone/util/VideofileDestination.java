package org.arquillian.cube.docker.drone.util;

import org.arquillian.cube.docker.drone.CubeDroneConfiguration;
import org.jboss.arquillian.test.spi.event.suite.After;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public class VideoFileDestination {

    private static final File DEFAULT_LOCATION_OUTPUT_MAVEN = new File("target");
    private static final File DEFAULT_LOCATION_OUTPUT_GRADLE = new File("build");

    private static final File MAVEN_REPORT_DIR = new File(DEFAULT_LOCATION_OUTPUT_MAVEN, "surefire-reports");
    private static final File MAVEN_FAILSAFE_REPORT_DIR = new File(DEFAULT_LOCATION_OUTPUT_MAVEN, "failsafe-reports");
    private static final File GRADLE_REPORT_DIR = new File(DEFAULT_LOCATION_OUTPUT_GRADLE, "test-results");

    private VideoFileDestination() {
        super();
    }

    public static Path getFinalLocation(After afterTestMethod, CubeDroneConfiguration cubeDroneConfiguration) {
        return resolveTargetDirectory(cubeDroneConfiguration).resolve(getFinalVideoName(afterTestMethod));
    }

    private static Path resolveTargetDirectory(CubeDroneConfiguration cubeDroneConfiguration) {
        if (cubeDroneConfiguration.isVideoOutputDirectorySet()) {
            return Paths.get(cubeDroneConfiguration.getFinalDirectory());
        } else {
            if (MAVEN_REPORT_DIR.exists()) {
                return MAVEN_REPORT_DIR.toPath();
            } else {
                if (MAVEN_FAILSAFE_REPORT_DIR.exists()) {
                    return MAVEN_FAILSAFE_REPORT_DIR.toPath();
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
        }

        if (!DEFAULT_LOCATION_OUTPUT_MAVEN.mkdirs()) {
            throw new IllegalArgumentException("Couldn't create directory for storing videos");
        }
        return DEFAULT_LOCATION_OUTPUT_MAVEN.toPath();
    }

    private static String getFinalVideoName(After afterTestMethod) {

        final String className = afterTestMethod.getTestClass().getName().replace('.', '_');
        final String methodName = afterTestMethod.getTestMethod().getName();

        return className + "_" + methodName + ".flv";

    }

}
