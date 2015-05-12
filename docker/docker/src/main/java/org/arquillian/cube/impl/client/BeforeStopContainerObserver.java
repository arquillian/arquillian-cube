package org.arquillian.cube.impl.client;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import org.arquillian.cube.impl.docker.DockerClientExecutor;
import org.arquillian.cube.impl.util.IOUtil;
import org.arquillian.cube.spi.Cube;
import org.arquillian.cube.spi.CubeRegistry;
import org.arquillian.cube.spi.event.lifecycle.BeforeStop;
import org.jboss.arquillian.core.api.annotation.Observes;

public class BeforeStopContainerObserver {

    private static final String BEFORE_STOP = "beforeStop";
    private static final String COPY = "copy";
    private static final String LOG = "log";
    private static final String TO = "to";
    private static final String FROM = "from";
    private static final String FOLLOW = "follow";
    private static final String STDOUT = "stdout";
    private static final String STDERR = "stderr";
    private static final String TIMESTAMPS = "timestamps";
    private static final String TAIL = "tail";

    @SuppressWarnings("unchecked")
    public void processCommands(@Observes BeforeStop beforeStop, CubeRegistry cubeRegistry,
            DockerClientExecutor dockerClientExecutor) throws IOException {

        Cube cube = cubeRegistry.getCube(beforeStop.getCubeId());
        Map<String, Object> configuration = cube.configuration();

        if (configuration.containsKey(BEFORE_STOP)) {
            List<Map<String, Object>> beforeStopConfiguration = (List<Map<String, Object>>) configuration
                    .get(BEFORE_STOP);

            for (Map<String, Object> map : beforeStopConfiguration) {
                if (map.containsKey(COPY)) {
                    Map<String, Object> copyConfiguration = (Map<String, Object>) map.get(COPY);
                    executeCopyAction(dockerClientExecutor, beforeStop.getCubeId(), copyConfiguration);
                } else {
                    if (map.containsKey(LOG)) {
                        Map<String, Object> logConfiguration = (Map<String, Object>) map.get(LOG);
                        executeLogAction(dockerClientExecutor, beforeStop.getCubeId(), logConfiguration);
                    }
                }
            }
        }
    }

    private void executeLogAction(DockerClientExecutor dockerClientExecutor, String containerId, Map<String, Object> configurationParameters) throws IOException {
        String to = null;
        if(configurationParameters.containsKey(TO)) {
            to = (String) configurationParameters.get(TO);
        } else {
            throw new IllegalArgumentException(String.format("to property is mandatory when getting logs from container %s.", containerId));
        }

        boolean follow = false;
        boolean stderr = false;
        boolean stdout = false;
        boolean timestamps = false;
        int tail = -1;
        
        if(configurationParameters.containsKey(FOLLOW)) {
            follow = (boolean) configurationParameters.get(FOLLOW);
        }

        if(configurationParameters.containsKey(STDOUT)) {
            stdout = (boolean) configurationParameters.get(STDOUT);
        }

        if(configurationParameters.containsKey(STDERR)) {
            stderr = (boolean) configurationParameters.get(STDERR);
        }

        if(configurationParameters.containsKey(TIMESTAMPS)) {
            timestamps = (boolean) configurationParameters.get(TIMESTAMPS);
        }

        if(configurationParameters.containsKey(TAIL)) {
            tail = (int) configurationParameters.get(TAIL);
        }

        Path toPath = Paths.get(to);
        File toPathFile = toPath.toFile();
        if(toPathFile.exists() && toPathFile.isDirectory()) {
            throw new IllegalArgumentException(String.format("%s parameter should be a file in log operation but you set an already existing directory not a file.", TO));
        }

        Path toDirectory = toPath.getParent();
        Files.createDirectories(toDirectory);
        dockerClientExecutor.copyLog(containerId, follow, stdout, stderr, timestamps, tail, new FileOutputStream(toPathFile));
    }
    private void executeCopyAction(DockerClientExecutor dockerClientExecutor, String containerId, Map<String, Object> configurationParameters) throws IOException {
        String to = null;
        String from = null;
        if(configurationParameters.containsKey(TO) && configurationParameters.containsKey(FROM)) {
            to = (String) configurationParameters.get(TO);
            from = (String) configurationParameters.get(FROM);
        } else {
            throw new IllegalArgumentException(String.format("to and from property is mandatory when copying files from container %s.", containerId));
        }

        InputStream response = dockerClientExecutor.getFileOrDirectoryFromContainerAsTar(containerId, from);
        Path toPath = Paths.get(to);
        File toPathFile = toPath.toFile();

        if(toPathFile.exists() && toPathFile.isFile()) {
            throw new IllegalArgumentException(String.format("%s parameter should be a directory in copy operation but you set an already existing file not a directory. Check %s in your local directory because currently is a file.", TO, toPath.normalize().toString()));
        }

        Files.createDirectories(toPath);

        IOUtil.untar(response, toPathFile);
    }
}
