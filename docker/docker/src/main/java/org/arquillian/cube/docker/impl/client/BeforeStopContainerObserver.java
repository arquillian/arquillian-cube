package org.arquillian.cube.docker.impl.client;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;

import org.arquillian.cube.docker.impl.client.config.BeforeStop;
import org.arquillian.cube.docker.impl.client.config.Copy;
import org.arquillian.cube.docker.impl.client.config.CubeContainer;
import org.arquillian.cube.docker.impl.client.config.Log;
import org.arquillian.cube.docker.impl.docker.DockerClientExecutor;
import org.arquillian.cube.docker.impl.model.DockerCube;
import org.arquillian.cube.impl.util.IOUtil;
import org.arquillian.cube.spi.Cube;
import org.arquillian.cube.spi.CubeRegistry;
import org.jboss.arquillian.core.api.annotation.Observes;

public class BeforeStopContainerObserver {

    public void processCommands(@Observes org.arquillian.cube.spi.event.lifecycle.BeforeStop beforeStop,
        CubeRegistry cubeRegistry,
        DockerClientExecutor dockerClientExecutor) throws IOException {

        Cube<CubeContainer> cube = cubeRegistry.getCube(beforeStop.getCubeId(), DockerCube.class);
        CubeContainer configuration = cube.configuration();

        if (configuration.getBeforeStop() != null) {
            Collection<BeforeStop> beforeStopConfiguration = configuration.getBeforeStop();

            for (BeforeStop map : beforeStopConfiguration) {
                if (map.getCopy() != null) {
                    Copy copyConfiguration = map.getCopy();
                    executeCopyAction(dockerClientExecutor, beforeStop.getCubeId(), copyConfiguration);
                } else {
                    if (map.getLog() != null) {
                        Log logConfiguration = map.getLog();
                        executeLogAction(dockerClientExecutor, beforeStop.getCubeId(), logConfiguration);
                    }
                }
              if(map.getCustomBeforeStopAction() != null) {
                  CustomBeforeStopAction customBeforeStopAction = map.getCustomBeforeStopAction();
                  executeCustomBeforeStopAction(dockerClientExecutor,beforeStop.getCubeId(),customBeforeStopAction);
              }
            }
        }
    }

    private void executeCustomBeforeStopAction(DockerClientExecutor dockerClientExecutor, String containerId, CustomBeforeStopAction customBeforeStopAction) {
        BeforeStopActionFactory.create(dockerClientExecutor, containerId,customBeforeStopAction).doBeforeStop();

    }

    private void executeLogAction(DockerClientExecutor dockerClientExecutor, String containerId, Log configurationParameters) throws IOException {
        String to = null;
        if (configurationParameters.getTo() != null) {
            to = configurationParameters.getTo();
        } else {
            throw new IllegalArgumentException(
                String.format("to property is mandatory when getting logs from container %s.", containerId));
        }

        boolean follow = false;
        boolean stderr = false;
        boolean stdout = false;
        boolean timestamps = false;
        int tail = -1;

        if (configurationParameters.getFollow() != null) {
            follow = configurationParameters.getFollow();
        }

        if (configurationParameters.getStdout() != null) {
            stdout = configurationParameters.getStdout();
        }

        if (configurationParameters.getStderr() != null) {
            stderr = configurationParameters.getStderr();
        }

        if (configurationParameters.getTimestamps() != null) {
            timestamps = configurationParameters.getTimestamps();
        }

        if (configurationParameters.getTail() != null) {
            tail = configurationParameters.getTail();
        }

        Path toPath = Paths.get(to);
        File toPathFile = toPath.toFile();
        if (toPathFile.exists() && toPathFile.isDirectory()) {
            throw new IllegalArgumentException(String.format(
                "%s parameter should be a file in log operation but you set an already existing directory not a file.",
                "to"));
        }

        Path toDirectory = toPath.getParent();
        Files.createDirectories(toDirectory);
        dockerClientExecutor.copyLog(containerId, follow, stdout, stderr, timestamps, tail,
            new FileOutputStream(toPathFile));
    }

    private void executeCopyAction(DockerClientExecutor dockerClientExecutor, String containerId,
        Copy configurationParameters) throws IOException {
        String to = null;
        String from = null;
        if (configurationParameters.getTo() != null && configurationParameters.getFrom() != null) {
            to = configurationParameters.getTo();
            from = configurationParameters.getFrom();
        } else {
            throw new IllegalArgumentException(
                String.format("to and from property is mandatory when copying files from container %s.", containerId));
        }

        InputStream response = dockerClientExecutor.getFileOrDirectoryFromContainerAsTar(containerId, from);
        Path toPath = Paths.get(to);
        File toPathFile = toPath.toFile();

        if (toPathFile.exists() && toPathFile.isFile()) {
            throw new IllegalArgumentException(String.format(
                "%s parameter should be a directory in copy operation but you set an already existing file not a directory. Check %s in your local directory because currently is a file.",
                "to", toPath.normalize().toString()));
        }

        Files.createDirectories(toPath);

        IOUtil.untar(response, toPathFile);
    }
}
