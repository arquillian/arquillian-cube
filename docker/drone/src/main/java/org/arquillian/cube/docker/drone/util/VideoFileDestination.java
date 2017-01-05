package org.arquillian.cube.docker.drone.util;

import org.arquillian.cube.docker.drone.CubeDroneConfiguration;
import org.jboss.arquillian.test.spi.event.suite.After;
import org.jboss.arquillian.test.spi.event.suite.AfterSuite;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class VideoFileDestination {

    private static final File DEFAULT_LOCATION_OUTPUT_MAVEN = new File("target");
    private static final File DEFAULT_LOCATION_OUTPUT_GRADLE = new File("build");

    private VideoFileDestination() {
        super();
    }

    public static Path getFinalLocation(After afterTestMethod, CubeDroneConfiguration cubeDroneConfiguration) {
        return resolveTargetDirectory(cubeDroneConfiguration).resolve(getFinalVideoName(afterTestMethod));
    }

    public static Path resolveTargetDirectory(CubeDroneConfiguration cubeDroneConfiguration) {

        Path output;

        if (cubeDroneConfiguration.isVideoOutputDirectorySet()) {
            output = Paths.get(cubeDroneConfiguration.getFinalDirectory());
        } else {
            if (DEFAULT_LOCATION_OUTPUT_GRADLE.exists()) {
                output = DEFAULT_LOCATION_OUTPUT_GRADLE.toPath().resolve("reports").resolve("videos");
            } else {
                if (DEFAULT_LOCATION_OUTPUT_MAVEN.exists()) {
                    output = DEFAULT_LOCATION_OUTPUT_MAVEN.toPath().resolve("reports").resolve("videos");
                } else {
                    if (!DEFAULT_LOCATION_OUTPUT_MAVEN.mkdirs()) {
                        throw new IllegalArgumentException("Couldn't create directory for storing videos");
                    }
                    output = DEFAULT_LOCATION_OUTPUT_MAVEN.toPath().resolve("reports").resolve("videos");
                }
            }
        }

        try {
            Files.createDirectories(output);
        } catch (IOException e) {
            throw new IllegalArgumentException("Couldn't create directory for storing videos");
        }

        return output;
    }

    private static String getFinalVideoName(After afterTestMethod) {

        final String className = afterTestMethod.getTestClass().getName().replace('.', '_');
        final String methodName = afterTestMethod.getTestMethod().getName();

        return className + "_" + methodName + ".flv";

    }

}
